package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.CellType;
import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Pin;
import de.hsbi.lockgame.model.Position;
import de.hsbi.lockgame.model.Snake;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private Level levelWithoutPins() {
        CellType[][] cells = new CellType[5][5];

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                cells[x][y] = CellType.EMPTY;
            }
        }

        return new Level(5, 5, cells, List.of(), new Position(2, 2));
    }

    private Level levelWithWallAt(Position wallPosition) {
        CellType[][] cells = new CellType[5][5];

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                cells[x][y] = CellType.EMPTY;
            }
        }

        cells[wallPosition.x()][wallPosition.y()] = CellType.WALL;

        return new Level(5, 5, cells, List.of(), new Position(2, 2));
    }

    private Level levelWithPin(Pin pin) {
        CellType[][] cells = new CellType[5][5];

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                cells[x][y] = CellType.EMPTY;
            }
        }

        cells[pin.position().x()][pin.position().y()] = CellType.PIN_SLOT;

        return new Level(5, 5, cells, List.of(pin), new Position(2, 2));
    }

    private GameState state(Level level, Snake snake, List<Pin> pins, Direction direction) {
        return new GameState(level, snake, pins, GameState.Status.RUNNING, direction);
    }

    @Test
    void givenInitialState_whenCreated_thenStatusIsRunningAndDirectionIsNone() {
        Level level = levelWithoutPins();
        Snake snake = new Snake(List.of(level.snakeStart()));

        GameState state = new GameState(
            level,
            snake,
            level.pins(),
            GameState.Status.RUNNING,
            Direction.NONE
        );

        assertEquals(GameState.Status.RUNNING, state.status());
        assertEquals(Direction.NONE, state.pendingDirection());
        assertEquals(new Position(2, 2), state.snake().head());
    }

    @Test
    void givenNoDirection_whenTick_thenStateDoesNotChange() {
        Level level = levelWithoutPins();
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.NONE);

        GameState next = state.tick();

        assertSame(state, next);
    }

    @Test
    void givenDirectionRight_whenTick_thenSnakeMovesRightAndGrows() {
        Level level = levelWithoutPins();
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(new Position(3, 2), next.snake().head());
        assertEquals(2, next.snake().body().size());
        assertEquals(GameState.Status.RUNNING, next.status());
    }

    @Test
    void givenWallAhead_whenTick_thenSnakeDoesNotMoveAndDirectionIsCleared() {
        Level level = levelWithWallAt(new Position(3, 2));
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(new Position(2, 2), next.snake().head());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertEquals(GameState.Status.RUNNING, next.status());
    }

    @Test
    void givenNextPositionOutsideLevel_whenTick_thenGameIsLostOutOfBounds() {
        Level level = levelWithoutPins();
        Snake snake = new Snake(List.of(new Position(0, 0)));
        GameState state = state(level, snake, level.pins(), Direction.LEFT);

        GameState next = state.tick();

        assertEquals(GameState.Status.LOST_OUT_OF_BOUNDS, next.status());
    }

    @Test
    void givenSelfCollisionAhead_whenTick_thenGameIsLostSelfCollision() {
        Level level = levelWithoutPins();
        Snake snake = new Snake(List.of(
            new Position(2, 2),
            new Position(3, 2),
            new Position(3, 3)
        ));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(GameState.Status.LOST_SELF_COLLISION, next.status());
    }

    @Test
    void givenLowPinWithCorrectDirection_whenTick_thenPinIsActivatedAndSnakeDoesNotMove() {
        Pin pin = new Pin(new Position(3, 2), Pin.State.LOW, Direction.RIGHT);
        Level level = levelWithPin(pin);
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(new Position(2, 2), next.snake().head());
        assertEquals(Pin.State.HIGH, next.pins().getFirst().state());
        assertEquals(GameState.Status.WON, next.status());
    }

    @Test
    void givenLowPinWithWrongDirection_whenTick_thenPinBlocksAndDirectionIsCleared() {
        Pin pin = new Pin(new Position(3, 2), Pin.State.LOW, Direction.LEFT);
        Level level = levelWithPin(pin);
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(new Position(2, 2), next.snake().head());
        assertEquals(Pin.State.LOW, next.pins().getFirst().state());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertEquals(GameState.Status.RUNNING, next.status());
    }

    @Test
    void givenHighPinAhead_whenTick_thenPinBlocksAndSnakeDoesNotMove() {
        Pin pin = new Pin(new Position(3, 2), Pin.State.HIGH, Direction.RIGHT);
        Level level = levelWithPin(pin);
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(new Position(2, 2), next.snake().head());
        assertEquals(Direction.NONE, next.pendingDirection());
        assertEquals(Pin.State.HIGH, next.pins().getFirst().state());
    }

    @Test
    void givenTwoPins_whenOnlyOnePinActivated_thenGameContinuesRunning() {
        Pin firstPin = new Pin(new Position(3, 2), Pin.State.LOW, Direction.RIGHT);
        Pin secondPin = new Pin(new Position(1, 1), Pin.State.LOW, Direction.UP);

        CellType[][] cells = new CellType[5][5];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                cells[x][y] = CellType.EMPTY;
            }
        }
        cells[3][2] = CellType.PIN_SLOT;
        cells[1][1] = CellType.PIN_SLOT;

        Level level = new Level(5, 5, cells, List.of(firstPin, secondPin), new Position(2, 2));
        Snake snake = new Snake(List.of(new Position(2, 2)));
        GameState state = state(level, snake, level.pins(), Direction.RIGHT);

        GameState next = state.tick();

        assertEquals(Pin.State.HIGH, next.pins().get(0).state());
        assertEquals(Pin.State.LOW, next.pins().get(1).state());
        assertEquals(GameState.Status.RUNNING, next.status());
    }
}
