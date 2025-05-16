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

package team.idealstate.sugar.next.calculate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public enum Parentheses implements Symbol {
    LEFT("("),
    RIGHT(")");

    public static final Set<Character> KEYWORDS = Collections.unmodifiableSet(Arrays.stream(Parentheses.values())
            .map(Symbol::getSymbol)
            .map(String::toCharArray)
            .map(chars -> {
                List<Character> characters = new ArrayList<>(chars.length);
                for (char c : chars) {
                    characters.add(c);
                }
                return characters;
            })
            .flatMap(List::stream)
            .collect(HashSet::new, HashSet::add, HashSet::addAll));

    private final String symbol;

    Parentheses(String symbol) {
        this.symbol = symbol;
    }
}
