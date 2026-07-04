import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * ============================================================================
 *  INTELLIGENT TASK PRIORITIZER WITH DEADLINES
 * ============================================================================
 *  Core Java + Java Swing + OOP + Custom Data Structure (Binary Min-Heap)
 *
 *  This single file contains three classes:
 *      1. Task        -> plain data model for a single task
 *      2. MinHeap     -> hand-built binary min-heap (NO java.util.PriorityQueue)
 *      3. MainDashboard -> Swing GUI that drives the whole application
 *
 *  Compile:  javac MainDashboard.java
 *  Run    :  java MainDashboard
 * ============================================================================
 */
public class MainDashboard {

    public static void main(String[] args) {
        // Swing GUIs must be built/updated on the Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> new TaskPrioritizerFrame().setVisible(true));
    }
}

// =============================================================================
// 1) TASK CLASS -- simple immutable-ish data holder for one task
// =============================================================================
class Task {

    // A running counter so every task gets a unique id automatically.
    private static int idCounter = 1;

    private final int id;
    private final String name;
    private final String description;
    private final int daysRemaining;
    private final int weight;          // Importance Weight: 1 (low) .. 5 (high)
    private final double priorityScore; // Lower score = higher real priority

    public Task(String name, String description, int daysRemaining, int weight) {
        this.id = idCounter++;
        this.name = name;
        this.description = description;
        this.daysRemaining = daysRemaining;
        this.weight = weight;
        // ---- Mathematical Priority Score ----
        // Score = (Days Remaining * 2) - Importance Weight
        // A closer deadline (small daysRemaining) OR higher weight -> lower score -> more urgent.
        this.priorityScore = (daysRemaining * 2.0) - weight;
    }

    public int getId()              { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public int getDaysRemaining()   { return daysRemaining; }
    public int getWeight()          { return weight; }
    public double getPriorityScore(){ return priorityScore; }

    @Override
    public String toString() {
        return String.format("[#%d] %s (Score: %.1f | Days: %d | Weight: %d)",
                id, name, priorityScore, daysRemaining, weight);
    }
}

// =============================================================================
// 2) CUSTOM MIN-HEAP CLASS -- built from scratch on top of an ArrayList
//    NOTE: java.util.PriorityQueue is intentionally NOT used anywhere.
// =============================================================================
class MinHeap {

    // Backing storage. Index 0 is always the root (smallest priority score).
    private final ArrayList<Task> heap;

    public MinHeap() {
        heap = new ArrayList<>();
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    // -------------------------------------------------------------------
    // Index helper formulas for a 0-indexed array-based binary heap:
    //   parent(i) = (i - 1) / 2
    //   leftChild(i)  = 2*i + 1
    //   rightChild(i) = 2*i + 2
    // -------------------------------------------------------------------
    private int parentIndex(int i)     { return (i - 1) / 2; }
    private int leftChildIndex(int i)  { return 2 * i + 1; }
    private int rightChildIndex(int i) { return 2 * i + 2; }

    private void swap(int i, int j) {
        Task temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }

    /**
     * insert(Task) -> O(log N)
     * Step 1: Append the new task at the very end of the array (last leaf).
     * Step 2: "Bubble up" (siftUp) that element until the min-heap property
     *         (parent.score <= child.score) is restored.
     */
    public void insert(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Cannot insert a null task into the heap.");
        }
        heap.add(task);                 // place at the end -> index = size-1
        siftUp(heap.size() - 1);        // restore heap order by moving it upward
    }

    /**
     * siftUp / bubbleUp
     * While the current node's score is smaller than its parent's score,
     * swap it with its parent and move up. This is what keeps the smallest
     * score bubbling toward the root after every insertion.
     */
    private void siftUp(int index) {
        while (index > 0) {
            int parent = parentIndex(index);
            if (heap.get(index).getPriorityScore() < heap.get(parent).getPriorityScore()) {
                swap(index, parent);
                index = parent; // continue checking from the new position
            } else {
                break; // heap property satisfied, stop
            }
        }
    }

    /**
     * peekMin() -> O(1)
     * The root of a min-heap is always the element with the lowest score.
     */
    public Task peekMin() {
        if (isEmpty()) {
            throw new IllegalStateException("Heap is empty: no task to peek at.");
        }
        return heap.get(0);
    }

    /**
     * extractMin() -> O(log N)
     * Step 1: Save the root (the task we want to return - it's the most urgent).
     * Step 2: Move the LAST element of the array into the root position.
     * Step 3: Shrink the array by removing the (now duplicated) last element.
     * Step 4: "Bubble down" (siftDown) the new root until heap order is restored.
     */
    public Task extractMin() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot extract from an empty heap.");
        }

        Task min = heap.get(0);
        int lastIndex = heap.size() - 1;

        Task lastTask = heap.remove(lastIndex); // remove last element from the array
        if (!heap.isEmpty()) {
            heap.set(0, lastTask); // move it to the root
            siftDown(0);           // restore heap order by moving it downward
        }
        return min;
    }

    /**
     * siftDown / bubbleDown
     * Compare the current node with both children. If either child is smaller,
     * swap with the SMALLER of the two children and continue downward.
     * This guarantees the min-heap property is restored in O(log N).
     */
    private void siftDown(int index) {
        int size = heap.size();
        while (true) {
            int left = leftChildIndex(index);
            int right = rightChildIndex(index);
            int smallest = index;

            if (left < size && heap.get(left).getPriorityScore() < heap.get(smallest).getPriorityScore()) {
                smallest = left;
            }
            if (right < size && heap.get(right).getPriorityScore() < heap.get(smallest).getPriorityScore()) {
                smallest = right;
            }

            if (smallest == index) {
                break; // both children are >= current node -> heap property restored
            }
            swap(index, smallest);
            index = smallest; // continue sifting down from the swapped position
        }
    }

    /**
     * Returns a snapshot of the tasks in their EXACT current internal array
     * layout (level-order / array order), so the GUI can visualize the raw
     * heap array to demonstrate the underlying tree structure.
     */
    public ArrayList<Task> getHeapArraySnapshot() {
        return new ArrayList<>(heap);
    }
}

// =============================================================================
// 3) MAIN SWING GUI -- dual pane dashboard
// =============================================================================
class TaskPrioritizerFrame extends JFrame {

    // ---- Core data structure driving the whole app ----
    private final MinHeap taskHeap = new MinHeap();

    // ---- Left panel (input form) components ----
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JSpinner daysSpinner;
    private JSlider weightSlider;

    // ---- Right panel (visualization) components ----
    private JTextArea nextTaskDisplay;
    private JLabel heapSizeLabel;
    private DefaultTableModel heapTableModel;
    private JTable heapTable;

    private static final Color BG_DARK      = new Color(30, 32, 36);
    private static final Color PANEL_DARK   = new Color(40, 43, 48);
    private static final Color ACCENT       = new Color(66, 133, 244);
    private static final Color ACCENT_RED   = new Color(219, 68, 55);
    private static final Color TEXT_LIGHT   = new Color(230, 230, 230);

    public TaskPrioritizerFrame() {
        super("Intelligent Task Prioritizer with Deadlines");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 680);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(BG_DARK);

        root.add(buildInputPanel(), BorderLayout.WEST);
        root.add(buildVisualizationPanel(), BorderLayout.CENTER);

        add(root, BorderLayout.CENTER);
        refreshHeapView(); // initialize empty-state UI
    }

    // -------------------------------------------------------------------
    // LEFT PANEL: Task input form
    // -------------------------------------------------------------------
    private JPanel buildInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 63, 68), 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        panel.setPreferredSize(new Dimension(340, 0));

        JLabel title = sectionTitle("📝  Add New Task");
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));

        panel.add(fieldLabel("Task Name"));
        nameField = new JTextField();
        styleTextField(nameField);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));

        panel.add(fieldLabel("Description"));
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(new Color(52, 55, 60));
        descriptionArea.setForeground(TEXT_LIGHT);
        descriptionArea.setCaretColor(TEXT_LIGHT);
        descriptionArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        panel.add(descScroll);
        panel.add(Box.createVerticalStrut(10));

        panel.add(fieldLabel("Days Remaining Until Deadline"));
        daysSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 3650, 1));
        styleSpinner(daysSpinner);
        panel.add(daysSpinner);
        panel.add(Box.createVerticalStrut(14));

        panel.add(fieldLabel("Importance Weight (1 = low, 5 = high)"));
        weightSlider = new JSlider(1, 5, 3);
        weightSlider.setMajorTickSpacing(1);
        weightSlider.setPaintTicks(true);
        weightSlider.setPaintLabels(true);
        weightSlider.setSnapToTicks(true);
        weightSlider.setBackground(PANEL_DARK);
        weightSlider.setForeground(TEXT_LIGHT);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        for (int i = 1; i <= 5; i++) {
            JLabel l = new JLabel(String.valueOf(i));
            l.setForeground(TEXT_LIGHT);
            labels.put(i, l);
        }
        weightSlider.setLabelTable(labels);
        weightSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        weightSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panel.add(weightSlider);
        panel.add(Box.createVerticalStrut(18));

        JButton addButton = new JButton("➕  Add Task to Heap");
        styleButton(addButton, ACCENT);
        addButton.addActionListener(this::handleAddTask);
        panel.add(addButton);

        panel.add(Box.createVerticalStrut(10));
        JLabel formula = new JLabel("<html><i>Score = (Days x 2) - Weight</i><br>"
                + "<i>Lower score = higher priority</i></html>");
        formula.setForeground(new Color(150, 150, 150));
        formula.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(formula);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // -------------------------------------------------------------------
    // RIGHT PANEL: Visualization + Actions
    // -------------------------------------------------------------------
    private JPanel buildVisualizationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_DARK);

        // ---- Top: Next Critical Task box + Execute button ----
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(PANEL_DARK);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 63, 68), 1, true),
                new EmptyBorder(14, 14, 14, 14)));

        JLabel nextTitle = sectionTitle("🚨  Next Critical Task (Heap Root)");
        topPanel.add(nextTitle, BorderLayout.NORTH);

        nextTaskDisplay = new JTextArea(3, 20);
        nextTaskDisplay.setEditable(false);
        nextTaskDisplay.setLineWrap(true);
        nextTaskDisplay.setWrapStyleWord(true);
        nextTaskDisplay.setBackground(new Color(52, 55, 60));
        nextTaskDisplay.setForeground(new Color(255, 210, 120));
        nextTaskDisplay.setFont(new Font("Monospaced", Font.BOLD, 14));
        nextTaskDisplay.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(nextTaskDisplay, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setBackground(PANEL_DARK);
        JButton executeButton = new JButton("✅  Execute / Complete Task");
        styleButton(executeButton, ACCENT_RED);
        executeButton.addActionListener(this::handleExtractMin);
        actionRow.add(executeButton);

        heapSizeLabel = new JLabel("Tasks in heap: 0");
        heapSizeLabel.setForeground(TEXT_LIGHT);
        heapSizeLabel.setFont(heapSizeLabel.getFont().deriveFont(Font.BOLD, 13f));
        actionRow.add(heapSizeLabel);

        topPanel.add(actionRow, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        // ---- Center: raw heap array table (tree structure demo) ----
        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        tablePanel.setBackground(PANEL_DARK);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 63, 68), 1, true),
                new EmptyBorder(14, 14, 14, 14)));

        JLabel tableTitle = sectionTitle("🌳  Raw Heap Array (Level-Order Layout)");
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        String[] columns = {"Array Index", "Parent Idx", "Task Name", "Score", "Days", "Weight"};
        heapTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        heapTable = new JTable(heapTableModel);
        heapTable.setBackground(new Color(45, 48, 53));
        heapTable.setForeground(TEXT_LIGHT);
        heapTable.setGridColor(new Color(65, 68, 73));
        heapTable.setRowHeight(26);
        heapTable.getTableHeader().setBackground(new Color(55, 58, 63));
        heapTable.getTableHeader().setForeground(TEXT_LIGHT);
        heapTable.setSelectionBackground(ACCENT.darker());

        JScrollPane tableScroll = new JScrollPane(heapTable);
        tableScroll.getViewport().setBackground(new Color(45, 48, 53));
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Index 0 = root. For index i: left child = 2i+1, "
                + "right child = 2i+2, parent = (i-1)/2.</html>");
        hint.setForeground(new Color(150, 150, 150));
        tablePanel.add(hint, BorderLayout.SOUTH);

        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------
    // EVENT HANDLERS
    // -------------------------------------------------------------------
    private void handleAddTask(ActionEvent e) {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();

        // ---- Basic input validation (edge case handling) ----
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Task Name cannot be empty.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int daysRemaining;
        try {
            // JSpinner already restricts to integers via SpinnerNumberModel,
            // but we still guard defensively in case of unexpected state.
            daysRemaining = (Integer) daysSpinner.getValue();
            if (daysRemaining < 0) {
                throw new NumberFormatException("Days remaining cannot be negative.");
            }
        } catch (NumberFormatException | ClassCastException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid non-negative number of days remaining.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int weight = weightSlider.getValue(); // guaranteed 1-5 by the slider itself

        if (description.isEmpty()) {
            description = "(no description provided)";
        }

        Task newTask = new Task(name, description, daysRemaining, weight);
        taskHeap.insert(newTask);

        // Clear the form for the next entry
        nameField.setText("");
        descriptionArea.setText("");
        daysSpinner.setValue(1);
        weightSlider.setValue(3);
        nameField.requestFocusInWindow();

        refreshHeapView();
    }

    private void handleExtractMin(ActionEvent e) {
        if (taskHeap.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "The heap is empty. There is no task to execute/complete.",
                    "Nothing to Extract", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Task completed = taskHeap.extractMin();
        JOptionPane.showMessageDialog(this,
                "Completed task:\n" + completed,
                "Task Executed", JOptionPane.INFORMATION_MESSAGE);
        refreshHeapView();
    }

    // -------------------------------------------------------------------
    // Refresh the "Next Critical Task" box, size label, and the raw array table
    // -------------------------------------------------------------------
    private void refreshHeapView() {
        if (taskHeap.isEmpty()) {
            nextTaskDisplay.setText("No tasks in the heap yet.\nAdd a task on the left to get started.");
        } else {
            Task root = taskHeap.peekMin();
            nextTaskDisplay.setText(
                    "Name       : " + root.getName() + "\n" +
                    "Score      : " + String.format("%.1f", root.getPriorityScore()) + "\n" +
                    "Days Left  : " + root.getDaysRemaining() + "  |  Weight: " + root.getWeight());
        }

        heapSizeLabel.setText("Tasks in heap: " + taskHeap.size());

        // Rebuild the table from the current internal array snapshot
        heapTableModel.setRowCount(0);
        ArrayList<Task> snapshot = taskHeap.getHeapArraySnapshot();
        for (int i = 0; i < snapshot.size(); i++) {
            Task t = snapshot.get(i);
            String parentIdx = (i == 0) ? "-" : String.valueOf((i - 1) / 2);
            heapTableModel.addRow(new Object[]{
                    i, parentIdx, t.getName(),
                    String.format("%.1f", t.getPriorityScore()),
                    t.getDaysRemaining(), t.getWeight()
            });
        }
    }

    // -------------------------------------------------------------------
    // UI styling helpers
    // -------------------------------------------------------------------
    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_LIGHT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(190, 190, 190));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(new Color(52, 55, 60));
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setBorder(new EmptyBorder(8, 8, 8, 8));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(new Color(52, 55, 60));
            tf.setForeground(TEXT_LIGHT);
            tf.setCaretColor(TEXT_LIGHT);
        }
    }

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
    }
}
