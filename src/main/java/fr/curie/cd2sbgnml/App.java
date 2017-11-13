package fr.curie.cd2sbgnml;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;

public class App extends Application {

    /**
     * This enum describes the 2 possible directions of convertion.
     */
    public enum ConvertionChoice {
        CD2SBGN,
        SBGN2CD;

        @Override
        public String toString() {
            switch (this) {
                case CD2SBGN: return "CellDesigner -> SBGN-ML";
                case SBGN2CD: return "SBGN-ML -> CellDesigner";
            }
            throw new IllegalArgumentException("No valid enum was given");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CellDesigner <-> SBGN-ML");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10, 10, 10, 10));


        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        vbox.getChildren().add(grid);


        // --- 0th row --- //
        Label directionLabel = new Label("Convertion Direction:");
        grid.add(directionLabel, 0, 0);

        ChoiceBox directionChoice = new ChoiceBox<>(FXCollections.observableArrayList(
                ConvertionChoice.CD2SBGN.toString(),
                ConvertionChoice.SBGN2CD.toString())
        );
        grid.add(directionChoice, 1, 0);
        directionChoice.getSelectionModel().selectFirst(); // set first as default

        // --- 1st row --- //
        Label inputFileLabel = new Label("Input File:");
        grid.add(inputFileLabel, 0, 1);

        TextField inputFileText = new TextField();
        grid.add(inputFileText, 1, 1);

        FileChooser inputFileChooser = new FileChooser();

        Button inputFileOpenButton = new Button("Choose file");
        grid.add(inputFileOpenButton, 2, 1);
        inputFileOpenButton.setOnAction(
                e -> {
                    File file = inputFileChooser.showOpenDialog(primaryStage);
                    if (file != null) {
                        inputFileText.setText(file.getAbsolutePath());
                    }
                });

        // --- 2nd row --- //
        Label outputFileLabel = new Label("Output File:");
        grid.add(outputFileLabel, 0, 2);

        TextField outputFileText = new TextField();
        grid.add(outputFileText, 1, 2);

        FileChooser outputFileChooser = new FileChooser();

        Button outputFileOpenButton = new Button("Save to");
        grid.add(outputFileOpenButton, 2, 2);
        outputFileOpenButton.setOnAction(
                e -> {
                    File file = outputFileChooser.showSaveDialog(primaryStage);
                    if (file != null) {
                        outputFileText.setText(file.getAbsolutePath());
                    }
                });

        // --- final row --- //
        Button convertButton = new Button("Convert");
        grid.add(convertButton, 1, 4, 3,1);
        convertButton.setOnAction(e -> {

            // check arguments
            if(inputFileText.getText().isEmpty()) {
                throw new IllegalArgumentException("No input file provided");
            }
            if(outputFileText.getText().isEmpty()) {
                throw new IllegalArgumentException("No output file provided");
            }

            if(directionChoice.getValue().equals(ConvertionChoice.CD2SBGN.toString())) {
                System.out.println("Convert button clicked, launch script");
                Cd2SbgnmlScript.convert(inputFileText.getText(), outputFileText.getText());
            }
            else if(directionChoice.getValue().equals(ConvertionChoice.SBGN2CD.toString())) {
                System.out.println("Convert button clicked, launch script");
                Sbgnml2CdScript.convert(inputFileText.getText(), outputFileText.getText());
            }
            else {
                throw new RuntimeException("That shouldn't happen.");
            }

        });

        // --- console --- //
        /*
            does not work properly, makes the gui freeze
         */
        /*TextArea console = new TextArea();
        console.setEditable(false);
        vbox.getChildren().add(console);
        PrintStream printStream = new PrintStream(new TextOutputStream(console));
        System.setOut(printStream);
        System.setErr(printStream);*/

        Scene scene = new Scene(vbox, 800, 400);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public class TextOutputStream extends OutputStream {
        TextArea textArea;

        public TextOutputStream(TextArea textArea){
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            // redirects data to the text area
            textArea.appendText(String.valueOf((char)b));
            // scrolls the text area to the end of data
            textArea.positionCaret(textArea.getText().length());
        }
    }
}
