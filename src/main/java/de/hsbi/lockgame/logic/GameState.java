package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.CellType;
import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Pin;
import de.hsbi.lockgame.model.Position;
import de.hsbi.lockgame.model.Snake;
import java.util.ArrayList;
import java.util.List;

public final class GameState {
    private final Level level;
    private final Snake snake;
    private final List<Pin> pins;
    private final Status status;
    private final Direction pendingDirection;

    public GameState(
        Level level, Snake snake, List<Pin> pins, Status status, Direction pendingDirection) {
        this.level = level;
        this.snake = snake;
        this.pins = List.copyOf(pins);
        this.status = status;
        this.pendingDirection = pendingDirection;
    }

    public Level level() {
        return level;
    }

    public Snake snake() {
        return snake;
    }

    public List<Pin> pins() {
        return pins;
    }

    public Status status() {
        return status;
    }

    public Direction pendingDirection() {
        return pendingDirection;
    }

    public GameState tick() {
        if (!status.isRunning() || pendingDirection == Direction.NONE) {
            return this;
        }

        Position nextPosition = snake.nextHead(pendingDirection);

        if (!level.isInside(nextPosition)) {
            return withStatus(Status.LOST_OUT_OF_BOUNDS);
        }

        if (level.cellAt(nextPosition) == CellType.WALL) {
            return withDirection(Direction.NONE);
        }

        if (snake.occupies(nextPosition)) {
            return withStatus(Status.LOST_SELF_COLLISION);
        }

        Pin pin = pinAt(nextPosition);

        if (pin != null) {
            if (!pin.state().isSet() && pin.activationDirection() == pendingDirection) {
                List<Pin> updatedPins = activatePin(pin);

                Status newStatus =
                    updatedPins.stream().allMatch(p -> p.state().isSet())
                        ? Status.WON
                        : Status.RUNNING;

                return new GameState(level, snake, updatedPins, newStatus, Direction.NONE);
            }

            return withDirection(Direction.NONE);
        }

        Snake movedSnake = snake.grow(pendingDirection);

        return new GameState(level, movedSnake, pins, status, pendingDirection);
    }

    private Pin pinAt(Position position) {
        return pins.stream()
            .filter(pin -> pin.position().equals(position))
            .findFirst()
            .orElse(null);
    }

    private List<Pin> activatePin(Pin pinToActivate) {
        List<Pin> updatedPins = new ArrayList<>();

        for (Pin pin : pins) {
            if (pin.position().equals(pinToActivate.position())) {
                updatedPins.add(pin.withState(Pin.State.HIGH));
            } else {
                updatedPins.add(pin);
            }
        }

        return updatedPins;
    }

    private GameState withDirection(Direction direction) {
        return new GameState(level, snake, pins, status, direction);
    }

    private GameState withStatus(Status newStatus) {
        return new GameState(level, snake, pins, newStatus, pendingDirection);
    }

    public enum Status {
        RUNNING,
        WON,
        LOST_SELF_COLLISION,
        LOST_OUT_OF_BOUNDS;

        public boolean isRunning() {
            return this == RUNNING;
        }
    }
}
