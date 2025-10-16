package team.idealstate.sugar.next.calculate.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum OperatorAssociativity {

    NON,
    LEFT,
    RIGHT
}
