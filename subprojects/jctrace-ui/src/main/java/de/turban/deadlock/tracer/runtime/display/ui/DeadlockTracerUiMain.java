package de.turban.deadlock.tracer.runtime.display.ui;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.display.DataVisualizationLoader;
import de.turban.deadlock.tracer.runtime.display.DeadlockCalculator;
import de.turban.deadlock.tracer.runtime.display.ui.model.UiLocks;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class DeadlockTracerUiMain extends Application {

    private static Path databaseFileStatic;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Path databaseFile = databaseFileStatic;

        URL fxmlUrl = getClass().getResource("/de/turban/deadlock/tracer/runtime/display/ui/Appl.fxml");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(fxmlUrl);
        Parent root = loader.load();

        primaryStage.setTitle("Deadlock Tracer Report");

        DeadlockCalculator calc = loadDatabaseAsync(databaseFile);

        // Give the controller access to the main app.
        DeadlockViewController controller = loader.getController();
        UiLocks locks = new UiLocks(calc);
        controller.setDeadlocks(locks);


        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private DeadlockCalculator loadDatabaseAsync(Path databaseFile) {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(ForkJoinPool.commonPool());
        ListenableFuture<DeadlockCalculator> res = executor.submit(() -> {
            IDeadlockDataResolver resolver = new DataVisualizationLoader().loadDatabase(databaseFile);
            DeadlockCalculator calc = new DeadlockCalculator(resolver);
            calc.calculateDeadlocks();
            return calc;
        });

        res.addListener(() -> {

            System.out.println("Database loaded from UI");

        }, executor);


        try {
            return res.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Deadlock Tracer Ui started...");
            if (args.length == 0) {
                System.out.println("Please specify a Database file as first argument.");
                System.exit(1);
            }

            databaseFileStatic = new File(args[0]).toPath();
            launch(args);
        }catch (Throwable ex){
            System.err.println("Unhandled exception occurred, shutting down.");
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
