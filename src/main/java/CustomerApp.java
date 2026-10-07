import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Run (JavaFX SDK required):
 *   java --module-path /path/to/javafx/lib --add-modules javafx.controls CustomerApp.java
 */
public class CustomerApp extends Application {

    // ---- 2. Model ---------------------------------------------------------
    public static class Customer {
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty province = new SimpleStringProperty();

        public Customer(String name, String province) {
            this.name.set(name);
            this.province.set(province);
        }
        public String getName() { return name.get(); }
        public String getProvince() { return province.get(); }
        public StringProperty nameProperty() { return name; }
        public StringProperty provinceProperty() { return province; }
    }

    private static final List<String> PROVINCES = List.of(
            "Central", "Copperbelt Eastern", "Luapula", "Lusaka",
            "Muchinga", "Northern", "North-Western",
            "Southern", "Western");

    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    private TextField nameField;
    private ComboBox<String> provinceBox;
    private Label errorLabel;
    private TableView<Customer> table;
    private Button deleteButton;

    // ---- 4. Validation (static so it can be unit-tested) -------------------
    /** Returns an error message, or null if valid. */
    static String validateName(String name) {
        if (name == null || name.isBlank()) return "Name is required.";
        if (name.length() > 50) return "Name must be 50 characters or fewer.";
        if (!name.matches("\\p{L}[\\p{L} .'\\-]*"))
            return "Name may contain only letters, spaces, periods, apostrophes and hyphens.";
        return null;
    }

    static String validateProvince(String province) {
        return (province == null || !PROVINCES.contains(province)) ? "Please choose a province." : null;
    }

    @Override
    public void start(Stage stage) {
        // ---- 1. Form ------------------------------------------------------
        nameField = new TextField();
        nameField.setPromptText("Full name");
        Label nameLabel = new Label("_Name");
        nameLabel.setMnemonicParsing(true);
        nameLabel.setLabelFor(nameField);

        provinceBox = new ComboBox<>(FXCollections.observableArrayList(PROVINCES));
        provinceBox.setPromptText("Select province");
        provinceBox.setMaxWidth(Double.MAX_VALUE);
        Label provinceLabel = new Label("_Province");
        provinceLabel.setMnemonicParsing(true);
        provinceLabel.setLabelFor(provinceBox);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #b00020;");
        errorLabel.setWrapText(true);
        errorLabel.setMinHeight(Label.USE_PREF_SIZE);

        Button addButton = new Button("_Add");
        addButton.setMnemonicParsing(true);
        addButton.setOnAction(e -> onAdd());
        nameField.setOnAction(e -> onAdd()); // Enter in the name field adds

        // Clear error styling as soon as the user edits
        nameField.textProperty().addListener((o, a, b) -> clearError());
        provinceBox.valueProperty().addListener((o, a, b) -> clearError());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, nameLabel, nameField);
        form.addRow(1, provinceLabel, provinceBox);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(provinceBox, Priority.ALWAYS);

        // ---- 3. TableView -------------------------------------------------
        table = new TableView<>(customers);
        table.setPlaceholder(new Label("No customers yet."));
        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(220);
        TableColumn<Customer, String> provCol = new TableColumn<>("Province");
        provCol.setCellValueFactory(c -> c.getValue().provinceProperty());
        provCol.setPrefWidth(220);
        table.getColumns().add(nameCol);
        table.getColumns().add(provCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---- 5. Delete with confirmation ---------------------------------
        deleteButton = new Button("_Delete");
        deleteButton.setMnemonicParsing(true);
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(e -> onDelete());
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) onDelete();
        });

        HBox buttons = new HBox(10, addButton, deleteButton);

        VBox root = new VBox(12, form, errorLabel, buttons, table);
        root.setPadding(new Insets(16));

        stage.setTitle("Customers");
        stage.setScene(new Scene(root, 520, 480));
        stage.show();
        nameField.requestFocus();
    }

    private void onAdd() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String province = provinceBox.getValue();

        String err = validateName(name);
        if (err != null) { showError(err, nameField); return; }

        err = validateProvince(province);
        if (err != null) { showError(err, provinceBox); return; }

        boolean duplicate = customers.stream().anyMatch(c ->
                c.getName().equalsIgnoreCase(name) && c.getProvince().equals(province));
        if (duplicate) { showError("That customer is already in the list.", nameField); return; }

        customers.add(new Customer(name, province));
        nameField.clear();
        provinceBox.setValue(null);
        clearError();
        nameField.requestFocus(); // ready for the next entry
    }

    private void onDelete() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + " (" + selected.getProvince() + ")?",
                delete, ButtonType.CANCEL);
        alert.setTitle("Confirm deletion");
        alert.setHeaderText(null);
        alert.initOwner(table.getScene().getWindow());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == delete) {
            int idx = table.getSelectionModel().getSelectedIndex();
            customers.remove(selected);
            if (!customers.isEmpty()) {
                table.getSelectionModel().select(Math.min(idx, customers.size() - 1));
            }
        }
        table.requestFocus();
    }

    // ---- Error helpers -----------------------------------------------------
    private void showError(String message, Node offender) {
        clearError();
        errorLabel.setText(message);
        offender.setStyle("-fx-border-color: #b00020; -fx-border-width: 2; -fx-border-radius: 3;");
        offender.setAccessibleHelp(message); // screen readers
        offender.requestFocus();
    }

    private void clearError() {
        errorLabel.setText("");
        nameField.setStyle("");
        provinceBox.setStyle("");
    }

    public static void main(String[] args) {
        launch(args);
    }
}