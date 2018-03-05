package fr.curie.cd2sbgnml;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static fr.curie.cd2sbgnml.App.ConvertionChoice.*;

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
                case CD2SBGN: return "CellDesigner ➡ SBGN-ML";
                case SBGN2CD: return "SBGN-ML ➡ CellDesigner";
            }
            throw new IllegalArgumentException("No valid enum was given");
        }
    }

    private ObjectProperty<ConvertionChoice> directionChoice = new SimpleObjectProperty<>(CD2SBGN);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CellDesigner ⇄ SBGN-ML");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20, 20, 20, 20));
        vbox.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        vbox.getChildren().add(grid);

        Button cd2sbgnRadio = new Button(CD2SBGN.toString());
        Button sbgn2cdRadio = new Button(SBGN2CD.toString());

        TextField inputFileText = new TextField();
        TextField outputFileText = new TextField();
        TextField logFileText = new TextField();


        cd2sbgnRadio.getStyleClass().add("toggle");
        cd2sbgnRadio.setPrefWidth(250);
        cd2sbgnRadio.setPrefHeight(36d);
        cd2sbgnRadio.setStyle("-fx-background-color: #1976d2");
        cd2sbgnRadio.setOnAction(e -> directionChoice.setValue(CD2SBGN));
        Animation b1select = buildAnimation(cd2sbgnRadio, true);
        Animation b1unselect = buildAnimation(cd2sbgnRadio, false);


        sbgn2cdRadio.getStyleClass().add("toggle");
        sbgn2cdRadio.setPrefWidth(250);
        sbgn2cdRadio.setPrefHeight(36d);
        sbgn2cdRadio.setOnAction(e -> directionChoice.setValue(SBGN2CD));
        Animation b2select = buildAnimation(sbgn2cdRadio, true);
        Animation b2unselect = buildAnimation(sbgn2cdRadio, false);


        // event on direction change
        directionChoice.addListener((observable, oldValue, newValue) -> {
            if(newValue == SBGN2CD) {
                b1unselect.play();
                b2select.play();
            }
            else {
                b1select.play();
                b2unselect.play();
            }
        });

        grid.add(cd2sbgnRadio, 1, 0);
        grid.add(sbgn2cdRadio, 1, 1);
        GridPane.setMargin(sbgn2cdRadio, new Insets(0,0,20,0));



        // --- 1st row --- //
        Label inputFileLabel = new Label("Input File");
        inputFileLabel.getStyleClass().addAll("bold");
        inputFileLabel.setPrefWidth(150);
        inputFileLabel.setAlignment(Pos.CENTER_RIGHT);
        grid.add(inputFileLabel, 0, 2);
        //GridPane.setHalignment(inputFileLabel, HPos.RIGHT);

        grid.add(inputFileText, 1, 2);

        FileChooser inputFileChooser = new FileChooser();

        Button inputFileOpenButton = GlyphsDude.createIconButton(FontAwesomeIcon.FOLDER_OPEN);
        inputFileOpenButton.getStyleClass().add("normal");
        grid.add(inputFileOpenButton, 2, 2);
        inputFileOpenButton.setOnAction(
                e -> {
                    File file = inputFileChooser.showOpenDialog(primaryStage);
                    if (file != null) {
                        inputFileText.setText(file.getAbsolutePath());
                        autoFillFromInputFile(file, outputFileText, logFileText);
                        inputFileText.positionCaret(inputFileText.getText().length());
                    }
                });

        // --- 2nd row --- //
        Label outputFileLabel = new Label("Output File");
        outputFileLabel.getStyleClass().addAll("bold");
        grid.add(outputFileLabel, 0, 3);
        GridPane.setHalignment(outputFileLabel, HPos.RIGHT);

        grid.add(outputFileText, 1, 3);

        FileChooser outputFileChooser = new FileChooser();

        Button outputFileOpenButton = GlyphsDude.createIconButton(FontAwesomeIcon.FOLDER_OPEN);
        outputFileOpenButton.getStyleClass().add("normal");
        grid.add(outputFileOpenButton, 2, 3);
        outputFileOpenButton.setOnAction(
                e -> {
                    File file = outputFileChooser.showSaveDialog(primaryStage);
                    if (file != null) {
                        outputFileText.setText(file.getAbsolutePath());
                        outputFileText.positionCaret(outputFileText.getText().length());
                    }
                });

        // --- 3rd row --- //
        Label logFileLabel = new Label("Log File");
        logFileLabel.getStyleClass().addAll("bold");
        grid.add(logFileLabel, 0, 4);
        GridPane.setHalignment(logFileLabel, HPos.RIGHT);

        grid.add(logFileText, 1, 4);

        FileChooser logFileChooser = new FileChooser();

        Button logFileOpenButton = GlyphsDude.createIconButton(FontAwesomeIcon.FOLDER_OPEN);
        logFileOpenButton.getStyleClass().add("normal");
        grid.add(logFileOpenButton, 2, 4);
        logFileOpenButton.setOnAction(
                e -> {
                    File file = logFileChooser.showSaveDialog(primaryStage);
                    if (file != null) {
                        logFileText.setText(file.getAbsolutePath());
                        logFileText.positionCaret(logFileText.getText().length());
                    }
                });


        // --- final row --- //
        Button convertButton = new Button("Convert");
        convertButton.getStyleClass().addAll("normal", "important");
        grid.add(convertButton, 1, 5);
        GridPane.setHalignment(convertButton, HPos.CENTER);
        GridPane.setMargin(convertButton, new Insets(20,0,0,0));

        // info row
        final Label infoLabel = new Label();
        final ProgressIndicator progressWheel = new ProgressIndicator();
        progressWheel.setPrefHeight(20d);

        HBox infosBox = new HBox(infoLabel);
        infosBox.setAlignment(Pos.CENTER);
        grid.add(infosBox, 1, 6);
        GridPane.setHalignment(infosBox, HPos.CENTER);

        convertButton.setOnAction(e -> {

            //System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
            //System.setProperty("org.slf4j.simpleLogger.logFile", logFileText.getText());
            PrintStream out = null;
            try {
                out = new PrintStream(new FileOutputStream(logFileText.getText()));
            } catch (FileNotFoundException e1) {
                infoLabel.setText("No log file provided.");
                return;
            }
            System.setOut(out);
            System.setErr(System.out);


            // check arguments
            if(inputFileText.getText().isEmpty()) {
                infoLabel.setText("No input provided.");
                return;
            }
            if(outputFileText.getText().isEmpty()) {
                infoLabel.setText("No output provided.");
                return;
            }

            if(directionChoice.get() == CD2SBGN) {
                System.out.println("Convert button clicked, launch script");
                Task task = new Task<Void>() {
                    @Override
                    public Void call() {
                        Platform.runLater(() ->{
                            infoLabel.setText("");
                            infosBox.getChildren().add(progressWheel);
                        });
                        try {
                            Cd2SbgnmlScript.convert(inputFileText.getText(), outputFileText.getText());
                            Platform.runLater(() -> infoLabel.setText("Done."));
                        }
                        catch (Exception e) {
                            Platform.runLater(() -> infoLabel.setText("An exception occured, see the log."));
                        }
                        finally {
                            Platform.runLater(() -> infosBox.getChildren().remove(progressWheel));
                        }

                        return null;
                    }
                };
                new Thread(task).start();

            }
            else if(directionChoice.get() == SBGN2CD) {
                System.out.println("Convert button clicked, launch script");
                Task task = new Task<Void>() {
                    @Override
                    public Void call() {
                        Platform.runLater(() ->{
                            infoLabel.setText("");
                            infosBox.getChildren().add(progressWheel);
                        });
                        try {
                            Sbgnml2CdScript.convert(inputFileText.getText(), outputFileText.getText());
                            Platform.runLater(() -> infoLabel.setText("Done."));
                        }
                        catch (Exception e) {
                            Platform.runLater(() -> infoLabel.setText("An exception occured, see the log."));
                        }
                        finally {
                            Platform.runLater(() -> infosBox.getChildren().remove(progressWheel));
                        }

                        return null;
                    }
                };
                new Thread(task).start();

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


        // issue message
        Text issueMessage = new Text("If you encounter a problem, please open an issue on");
        issueMessage.getStyleClass().add("small");

        Hyperlink issueLink = new Hyperlink("github");
        issueLink.getStyleClass().add("small");
        issueLink.setStyle("-fx-underline: true;");

        TextFlow wholeMessage = new TextFlow(issueMessage, issueLink);
        wholeMessage.setPrefWidth(170d);

        HBox footer = new HBox(wholeMessage);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        issueLink.setOnMouseClicked(e -> {
            getHostServices().showDocument("https://github.com/royludo/cd2sbgnml/issues");
        });

        AnchorPane anchorPane = new AnchorPane(vbox, footer);
        AnchorPane.setTopAnchor(vbox, 20d);
        AnchorPane.setBottomAnchor(footer, 0d);
        AnchorPane.setRightAnchor(footer, 0d);

        Scene scene = new Scene(anchorPane, 610, 400);
        scene.getStylesheets().add(this.getClass().getResource("/guiStyle.css").toExternalForm());
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

    /**
     * The short color fade animation used for the translation direction choice buttons.
     */
    private Animation buildAnimation(Button button, boolean select) {
        final Animation animation = new Transition() {
            Color startColor, endColor;
            {
                setCycleDuration(Duration.millis(100));
                setInterpolator(Interpolator.LINEAR);
                String normalColor = "#373737";
                String selectedColor = "#1976d2";

                if(select) {
                    startColor = Color.web(normalColor);
                    endColor = Color.web(selectedColor);
                }
                else { // unselect
                    startColor = Color.web(selectedColor);
                    endColor = Color.web(normalColor);
                }

            }

            @Override
            protected void interpolate(double frac) {
                Color finalColor = startColor.interpolate(endColor, frac);
                button.setStyle("-fx-background-color: "+toRGBCode(finalColor));
            }
        };
        return animation;
    }

    private void autoFillFromInputFile(File inputFile, TextField outputField, TextField logField) {
        Path inputPath = inputFile.toPath().getParent();
        String inputName = inputFile.toPath().getFileName().toString();

        // strip extension
        String noExt;
        if(inputName.contains(".")) {
            noExt = inputName.substring(0, inputName.lastIndexOf('.'));
        }
        else {
            noExt = inputName;
        }

        String outputExt;
        if(directionChoice.get() == CD2SBGN) {
            outputExt = "sbgn";
        }
        else {
            outputExt = "xml";
        }

        Path outputFullPath = Paths.get(inputPath.toString(), noExt+"."+outputExt);
        Path logFullPath = Paths.get(inputPath.toString(), noExt+".log");

        outputField.setText(outputFullPath.toString());
        logField.setText(logFullPath.toString());

        outputField.positionCaret(outputField.getText().length());
        logField.positionCaret(logField.getText().length());
    }

    /**
     * From https://stackoverflow.com/a/18803814
     */
    private static String toRGBCode( Color color ) {
        return String.format( "#%02X%02X%02X",
                (int)( color.getRed() * 255 ),
                (int)( color.getGreen() * 255 ),
                (int)( color.getBlue() * 255 ) );
    }
}
