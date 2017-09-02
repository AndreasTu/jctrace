package de.turban.deadlock.tracer.runtime.display.ui.model;

import de.turban.deadlock.tracer.runtime.display.DeadlockCalculator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class UiLocks {

    private ObservableList<UiLock> locks;


    public UiLocks(DeadlockCalculator calc) {
        locks = FXCollections.observableArrayList();
        calc.getPossibleDeadLocks().stream().filter(l -> l.getLockerLocationIds().length != 0).forEach(e -> locks.add(new UiLock(calc.getResolver(), e)));
    }

    public ObservableList<UiLock> getLocks() {
        return locks;
    }
}
