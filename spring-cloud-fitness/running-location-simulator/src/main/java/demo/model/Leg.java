package demo.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Leg {
    private int id;
    private Point startPosition;
    private Point endPosition;
    private double length;
    private double direction;
}
