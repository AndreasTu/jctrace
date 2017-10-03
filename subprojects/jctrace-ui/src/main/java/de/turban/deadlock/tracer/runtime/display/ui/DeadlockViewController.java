package de.turban.deadlock.tracer.runtime.display.ui;

import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.IStackSample;
import de.turban.deadlock.tracer.runtime.display.ui.model.UiLock;
import de.turban.deadlock.tracer.runtime.display.ui.model.UiLocks;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


public class DeadlockViewController {

    @FXML
    ListView<String> otherLocationsList;

    @FXML
    ListView<String> otherThreadsList;

    @FXML
    TreeView<String> otherCallstacks;

    @FXML
    ListView<String> lockLocationsList;

    @FXML
    ListView<String> lockThreadsList;

    @FXML
    TreeView<String> lockCallstacks;

    @FXML
    TreeView<Object> deadlockTree;

    @FXML
    TreeView<Object> selectedDeadlockTree;


    public void setDeadlocks(UiLocks locks) {

        TreeItem<Object> root = new TreeItem<>();
        locks.getLocks().forEach(lock -> {
            treeNodeForLock(root, lock);

        });

        deadlockTree.setRoot(root);
        deadlockTree.getSelectionModel().selectedItemProperty().addListener((ob, old, newValue) -> {
            TreeItem<Object> selectedRoot = new TreeItem<>();
            if (newValue != null) {
                Object item = newValue.getValue();
                if (item instanceof UiLock) {
                    UiLock lock = (UiLock) item;

                    lockLocationsList.setItems(lock.getLocations());
                    lockThreadsList.setItems(lock.getThreads());
                    fillCallStack(lock, lockCallstacks);

                    lock.getPossibleDeadLockWithLocks().forEach(l -> treeNodeForLock(selectedRoot, l));

                }
            }
            selectedDeadlockTree.setRoot(selectedRoot);
            otherLocationsList.setItems(FXCollections.emptyObservableList());
            otherThreadsList.setItems(FXCollections.emptyObservableList());
            otherCallstacks.setRoot(new TreeItem<>());
        });

        selectedDeadlockTree.getSelectionModel().selectedItemProperty().addListener((ob, old, newValue) -> {
            if (newValue != null) {
                Object item = newValue.getValue();
                if (item instanceof UiLock) {
                    UiLock lock = (UiLock) item;
                    otherLocationsList.setItems(lock.getLocations());
                    otherThreadsList.setItems(lock.getThreads());
                    fillCallStack(lock, otherCallstacks);
                }
            }

        });


    }

    private void fillCallStack(UiLock lock, TreeView<String> control) {
        TreeItem<String> root = new TreeItem<>();
        IDeadlockDataResolver resolver = lock.getResolver();
        boolean elementAdded = false;
        int x = 1;
        for (IStackSample stack : lock.getLockCache().getStackSamples()) {
            if (hasLockPossibleDeadLocks(resolver, stack.getDependentLocks(), lock.getLockCache())) {
                TreeItem<String> stackEntryTree = new TreeItem<String>("Stack-Entry " + x + " Thread: " + resolver.getThreadCache().getThreadDescriptionById(stack.getThreadId()) + "");
                x++;

                stack.getStackTrace().forEach(elem -> {
                    if (elem == null) {
                        return;
                    }
                    String msg = elem.toString();

                    depLockSearch:
                    for (int depLockId : stack.getDependentLocks()) {
                        ILockCacheEntry depLock = resolver.getLockCache().getLockById(depLockId);
                        if (depLock != null) {
                            for (int locId : depLock.getLocationIds()) {
                                StackTraceElement traceElement = resolver.getLocationCache().getLocationById(locId);
                                if (traceElement != null) {
                                    if (elem.getClassName().equals(traceElement.getClassName())) {
                                        if (elem.getFileName().equals(traceElement.getFileName())) {
                                            if (elem.getMethodName().equals(traceElement.getMethodName())) {
                                                msg += "  =====> HoldingLock (id:" + depLockId + ") <=====";
                                                continue depLockSearch;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TreeItem<String> stackFrame = new TreeItem<String>(msg);
                    stackEntryTree.getChildren().add(stackFrame);
                });


                root.getChildren().add(stackEntryTree);
                elementAdded = true;
            }
        }
        if (!elementAdded) {
            TreeItem<String> noStack = new TreeItem<String>("No Callstack available. Please activate callstack collection for locations.");
            root.getChildren().add(noStack);
        }


        control.setRoot(root);
    }

    private boolean hasLockPossibleDeadLocks(IDeadlockDataResolver resolver, int[] dependentLocks, ILockCacheEntry lock) {
        for (int lockId : dependentLocks) {
            ILockCacheEntry depLock = resolver.getLockCache().getLockById(lockId);

            if (depLock != null) {
                if (!depLock.hasDependentLock(lock.getId())) {
                    continue;
                }
                return true;

            }

        }
        return false;
    }

    private void treeNodeForLock(TreeItem<Object> root, UiLock lock) {
        TreeItem<Object> l = new TreeItem<Object>(lock);
        l.getChildren().add(new TreeItem<>("LockClass: " + lock.getLockCache().getLockClass()));
        root.getChildren().add(l);
    }
}
