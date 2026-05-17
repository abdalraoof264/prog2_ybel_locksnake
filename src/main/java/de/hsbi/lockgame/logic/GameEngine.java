package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Snake;
import java.util.ArrayList;
import java.util.List;

public final class GameEngine implements DirectionObserver {
    private GameState state;
    private final List<GameStateObserver> observers = new ArrayList<>();

    public GameEngine(Level level) {
        this.state =
            new GameState(
                level,
                new Snake(List.of(level.snakeStart())),
                level.pins(),
                GameState.Status.RUNNING,
                Direction.NONE);
    }

    public GameState state() {
        return state;
    }

    public void addGameStateObserver(GameStateObserver observer) {
        observers.add(observer);
    }

    @Override
    public void update(Direction direction) {
        if (!state.status().isRunning()) {
            return;
        }

        state =
            new GameState(
                state.level(),
                state.snake(),
                state.pins(),
                state.status(),
                direction);

        notifyObservers();
    }

    public void tick() {
        state = state.tick();
        notifyObservers();
    }

    private void notifyObservers() {
        observers.forEach(observer -> observer.update(state));
    }
}
