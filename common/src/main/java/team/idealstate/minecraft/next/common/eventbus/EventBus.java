/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.minecraft.next.common.eventbus;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.stacktrace.StackTraceUtils;
import team.idealstate.minecraft.next.common.validate.Validation;

public abstract class EventBus {

    private static volatile EventBus INSTANCE = null;

    public static EventBus instance() {
        if (INSTANCE == null) {
            synchronized (EventBus.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SimpleEventBus();
                }
            }
        }
        return INSTANCE;
    }

    public abstract EventState publish(Event event);

    public abstract <T extends Event> boolean subscribe(
            Class<T> eventType, EventSubscriber<T> subscriber);

    public abstract boolean unsubscribe(EventSubscriber<?> subscriber);

    private static final class SimpleEventBus extends EventBus {
        private static final int SUBSCRIBERS_INITIAL_CAPACITY = 32;
        private static final Order DEFAULT_ORDER = Order.LAST_ORDER;
        private static final Comparator<EventSubscriber<?>> SUBSCRIBER_COMPARATOR =
                (first, second) -> {
                    Order firstOrder = first instanceof Order ? (Order) first : DEFAULT_ORDER;
                    Order secondOrder = second instanceof Order ? (Order) second : DEFAULT_ORDER;
                    return Order.COMPARATOR.compare(firstOrder, secondOrder);
                };
        private final Map<Class<?>, Set<EventSubscriber<?>>> subscribers =
                new ConcurrentHashMap<>(SUBSCRIBERS_INITIAL_CAPACITY);
        private final Set<Publishing> publishing = new CopyOnWriteArraySet<>();

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static EventState onEvent(Event event, Collection<EventSubscriber<?>> subscribers) {
            for (EventSubscriber subscriber : subscribers) {
                try {
                    subscriber.onEvent(event);
                } catch (Throwable e) {
                    Log.error(
                            String.format(
                                    "Exception while invoking subscriber \"%s\" for event \"%s\".\n"
                                            + "%s",
                                    subscriber, event, StackTraceUtils.makeDetails(e)));
                    return EventState.FAILURE;
                }
            }
            return EventState.SUCCESS;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static EventState onEvent(
                Event event, Cancelable cancelable, Collection<EventSubscriber<?>> subscribers) {
            if (cancelable.isCancelled()) {
                return EventState.CANCELLED;
            }
            for (EventSubscriber subscriber : subscribers) {
                try {
                    subscriber.onEvent(event);
                } catch (Throwable e) {
                    Log.error(
                            String.format(
                                    "Exception while invoking subscriber \"%s\" for event \"%s\".\n"
                                            + "%s",
                                    subscriber, event, StackTraceUtils.makeDetails(e)));
                    return EventState.FAILURE;
                }
                if (cancelable.isCancelled()) {
                    return EventState.CANCELLED;
                }
            }
            return EventState.SUCCESS;
        }

        @Override
        public EventState publish(Event event) {
            Validation.notNull(event, "event cannot be null.");
            final Publishing publishing = new Publishing(event);
            if (!this.publishing.add(publishing)) {
                Log.error(String.format("Circular event \"%s\" publishing detected.", event));
                return EventState.FAILURE;
            }
            Class<?> eventType = event.getClass();
            Collection<EventSubscriber<?>> subscribers = this.subscribers.get(eventType);
            try {
                if (subscribers != null && !subscribers.isEmpty()) {
                    subscribers =
                            subscribers.stream()
                                    .sorted(SUBSCRIBER_COMPARATOR)
                                    .collect(Collectors.toList());
                    if (!subscribers.isEmpty()) {
                        if (event instanceof Cancelable) {
                            return onEvent(event, (Cancelable) event, subscribers);
                        } else {
                            return onEvent(event, subscribers);
                        }
                    }
                }
                if (!(event instanceof Cancelable)) {
                    return EventState.SUCCESS;
                }
                return ((Cancelable) event).isCancelled()
                        ? EventState.CANCELLED
                        : EventState.SUCCESS;
            } finally {
                this.publishing.remove(publishing);
            }
        }

        @Override
        public <T extends Event> boolean subscribe(
                Class<T> eventType, EventSubscriber<T> subscriber) {
            Validation.notNull(eventType, "event type cannot be null.");
            Validation.notNull(subscriber, "subscriber cannot be null.");
            return this.subscribers
                    .computeIfAbsent(eventType, key -> new CopyOnWriteArraySet<>())
                    .add(subscriber);
        }

        @Override
        public boolean unsubscribe(EventSubscriber<?> subscriber) {
            Validation.notNull(subscriber, "subscriber cannot be null.");
            boolean removed = false;
            for (Set<EventSubscriber<?>> subscribers : this.subscribers.values()) {
                if (subscribers.remove(subscriber)) {
                    removed = true;
                }
            }
            return removed;
        }

        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        @ToString
        private static final class Publishing {
            @NonNull private final Event event;

            @Override
            public boolean equals(Object object) {
                if (object == null || getClass() != object.getClass()) return false;
                Publishing that = (Publishing) object;
                return Objects.equals(event, that.event);
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(event);
            }
        }
    }
}
