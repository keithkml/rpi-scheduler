/*
 *  Copyright (c) 2004, The University Scheduler Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the University Scheduler Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package edu.rpi.scheduler.ui.panels.courses;

import edu.rpi.scheduler.engine.SchedulerEngine;
import edu.rpi.scheduler.engine.SelectedCourse;
import edu.rpi.scheduler.schedb.CourseDescriptor;
import edu.rpi.scheduler.schedb.spec.ClassPeriod;
import edu.rpi.scheduler.schedb.spec.Department;
import edu.rpi.scheduler.schedb.spec.Professor;
import edu.rpi.scheduler.schedb.spec.Section;
import edu.rpi.scheduler.ui.MonitoredJFrame;
import edu.rpi.scheduler.ui.SchedulerUIPlugin;
import edu.rpi.scheduler.ui.SchedulingSession;
import edu.rpi.scheduler.ui.StringFormatType;
import edu.rpi.scheduler.ui.UITools;
import edu.rpi.scheduler.ui.WindowType;
import edu.rpi.scheduler.ui.panels.DefaultSubpanel;
import edu.rpi.scheduler.ui.panels.courses.indexer.CourseDocument;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchKey;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchType;
import edu.rpi.scheduler.ui.widgets.CourseInfoPanel;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static edu.rpi.scheduler.ui.panels.courses.SelectedCoursesList.COL_EXTRA;

public class CourseSelectionPanel extends DefaultSubpanel {
    //TOSMALL: highlight text that matches Quick Find text
    //TOSMALL: autocomplete / dropdown for Quick Find
    //TOSMALL: fix drag-and-drop selection messing with stuff
    //TOSMALL: fix delayed init()?

    /* Saved settings */
    private static final Preferences PREFERENCES
            = Preferences.userNodeForPackage(CourseSelectionPanel.class);
    private static final String PREF_SEARCH_TEXT = "search-text";
    private static final String PREF_SEARCH_TYPE = "search-type";
    private static final String PREF_SELECTED_DEPT = "selected-department";
    private static final String KEY_COURSE_VIEWER = "course-viewer";

    /* Other constants */
    private static final int MAX_MATCHLESS_SEARCHED_ENTRIES = 1000;

    /* UI components */
    private JComboBox departmentBox;
    private SearchTextField searchBox;
    private JTable selectedCoursesList;
    private JList possibleCoursesList;
    private JPanel mainPanel;
    private CourseInfoPanel courseInfoPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton viewButton;
    private JLabel whatsExtraLabel;

    /* Course preview */
    private CourseTimeViewerPanel courseViewerPanel;
    private JFrame courseViewerWindow;

    /* Data models */
    private SelectedCoursesList selectedCoursesModel;
    private PossibleCoursesListModel possibleCoursesModel;
    private DepartmentCBModel departmentModel;

    /* Popup menus */
    private JPopupMenu selectedCoursesPopupMenu;
    private JPopupMenu possibleCoursesPopupMenu;
    private JMenu professorPopupMenu;
    private int profMenuIndex;
    private JMenuItem singleProfessorMenuItem = null;
    private String lastSingleProfessorName = null;
    private SearchType profSearchType = null;

    /* UI miscellaneous fields */
    private boolean inited = false;
    private ConflictDetector conflictDetector;
    private AddCourseAction addAction;
    private RemoveCourseAction removeCourseAction;
    private ViewSectionsAction viewAction;
    private Map<SearchType, Set<SearchKey>> searchTypes;

    /* Initialization */
    public synchronized void init() {
        if (inited) return;
        inited = true;

        setLayout(new BorderLayout());
        add(mainPanel);

        initSearchBox();

        initDepartmentBox();

        initPossibleCoursesList();

        initSelectedCoursesList();

        conflictDetector = new ConflictDetector(getSession(), selectedCoursesModel);
        conflictDetector.addListener(new ConflictListener() {
            public void conflictsUpdated(ConflictDetector detector) {
                possibleCoursesModel.notifyChanged();
            }
        });

        courseInfoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        courseInfoPanel.setSession(getSession());
        courseInfoPanel.setConflictDetector(conflictDetector);
        courseInfoPanel.setSelectedCoursesList(selectedCoursesModel);

        initPopupMenus();

        initDragonDrop();

        initButtons();

        whatsExtraLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        whatsExtraLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(CourseSelectionPanel.this,
                        "If you don't know what you want to take, add the\n"
                        + "courses you're not sure about as Extra courses.\n"
                        + "The Scheduler will show you schedules with and\n"
                        + "without Extra courses.", "Extra Courses",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        setPossibleCoursesListPrototypeValue();
        loadSearchBoxSettings();
        updateDept();
    }

    private void initSearchBox() {
        searchBox.init();
        searchBox.addSearchTextFieldListener(new SearchTextFieldListener() {
            public void searchStringChanged(SearchTextField field, String string) {
                List<CourseDescriptor> selected = getSelectedPossibleCourses();
                possibleCoursesModel.setFilter(string);
                setSelectedPossibleCourses(selected);
            }

            public void searchTypeChanged(SearchTextField field, SearchType type) {
                List<CourseDescriptor> selected = getSelectedPossibleCourses();
                possibleCoursesModel.setFilterKeys(searchTypes.get(type));
                setSelectedPossibleCourses(selected);
            }
        });
        SchedulerUIPlugin plugin = getSession().getUIPlugin();
        Map<SearchType,Set<SearchKey>> types = plugin.getCourseSearchTypes();
        searchTypes = types;
        searchBox.setSearchTypes(types.keySet());
//        searchBox.setSearchType();
        findProfessorSearchType();
    }

    private void setSelectedPossibleCourses(List<CourseDescriptor> selected) {
        ListSelectionModel selModel = possibleCoursesList.getSelectionModel();
        int first = -1;
        for (CourseDescriptor cd : selected) {
            int index = possibleCoursesModel.indexOf(cd);
            if (index != -1) {
                selModel.addSelectionInterval(index, index);
                if (first == -1 || index < first) first = index;
            }
        }
//        if (first != -1) {
//            possibleCoursesList.ensureIndexIsVisible(first);
//        }
    }

    private void initDepartmentBox() {
        departmentModel = new DepartmentCBModel(getSchedulerData());
        departmentBox.setRenderer(new DeptBoxRenderer());
        departmentBox.setModel(departmentModel);
        departmentBox.setMaximumRowCount(13);

        departmentBox.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                JComboBox box = (JComboBox) e.getSource();
                Object comp = box.getUI().getAccessibleChild(box, 0);
                if (!(comp instanceof JPopupMenu)) return;
                JComponent scrollPane = (JComponent) ((JPopupMenu) comp).getComponent(0);
                Dimension size = new Dimension();
                size.width = box.getPreferredSize().width;
                size.height = scrollPane.getPreferredSize().height;
                scrollPane.setPreferredSize(size);
                scrollPane.setMaximumSize(size);
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        String abbr = PREFERENCES.get(PREF_SELECTED_DEPT, null);
        if (abbr != null) {
            departmentModel.setSelectedDept(abbr);
        }
    }

    private void initPossibleCoursesList() {
        possibleCoursesModel = new PossibleCoursesListModel(getSession());
        departmentBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateDept();
            }
        });

        possibleCoursesList.setModel(possibleCoursesModel);
        possibleCoursesList.setCellRenderer(new CourseRenderer());
        possibleCoursesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateAddButtons();
            }
        });
        possibleCoursesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    addOrRemoveSelectedPossibleCourses();
                }
            }

            public void mouseReleased(MouseEvent e) {
                checkPopupTrigger(e);
            }

            public void mousePressed(MouseEvent e) {
                checkPopupTrigger(e);
            }

            private void checkPopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    possibleCoursesPopupMenu.show(e.getComponent(),
                            e.getX(), e.getY());
                }
            }

        });
        SearchType item = searchBox.getSearchType();
        if (item != null) {
            possibleCoursesModel.setFilterKeys(searchTypes.get(item));
        }
    }

    private void setPossibleCoursesListPrototypeValue() {
        if (possibleCoursesModel.getSize() > 0) {
            CourseDescriptor firstCourse = possibleCoursesModel.getCourseAt(0);
            PossibleCoursesListModel.CourseHolder holder
                    = new PossibleCoursesListModel.CourseHolder(firstCourse);
            possibleCoursesList.setPrototypeCellValue(holder);
        }
    }

    private void initSelectedCoursesList() {
        selectedCoursesModel = new SelectedCoursesList(getSession());
        selectedCoursesList.setModel(selectedCoursesModel);
        selectedCoursesModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                getSchedulerPanel().updateNextStatus();
            }
        });
        ListSelectionModel selModel = selectedCoursesList.getSelectionModel();
        selModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int[] rows = selectedCoursesList.getSelectedRows();
                List<SelectedCourse> selected
                        = selectedCoursesModel.getCourses(rows);
                int numSelected = selected.size();
                if (numSelected == 0) {
//                    possibleCoursesList.setSelectedIndex(-1);

                } else if (numSelected == 1) {
                    CourseDescriptor course = selected.get(0).getCourse();
                    int index = possibleCoursesModel.indexOf(course);
                    possibleCoursesList.setSelectedIndex(index);
                    ensureCourseIsVisible(course);
                }
            }
        });

        TableColumnModel cols = selectedCoursesList.getColumnModel();
        TableColumn courseCol = cols.getColumn(SelectedCoursesList.COL_COURSE);
        courseCol.setMinWidth(25);

        TableColumn extraCol = cols.getColumn(COL_EXTRA);
        TableCellRenderer headerRenderer = extraCol.getHeaderRenderer();
        if (headerRenderer == null) {
            headerRenderer = selectedCoursesList.getTableHeader().getDefaultRenderer();
        }
        Component renderer = headerRenderer.getTableCellRendererComponent(
                selectedCoursesList,
                selectedCoursesList.getColumnName(COL_EXTRA),
                false, true, 0, COL_EXTRA);
        Dimension preferredSize = renderer.getPreferredSize();
        int width = preferredSize.width + 3;
        if (width < 20) width = 20;
        extraCol.sizeWidthToFit();
//        if (extraCol.getPreferredWidth() > width) width = extraCol.getPreferredWidth();
        extraCol.setMaxWidth(width);
        extraCol.setPreferredWidth(width);
        if (width > extraCol.getWidth()) extraCol.setWidth(width);
//        extraCol.setResizable(false);

        selectedCoursesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    removeSelectedSelectedCourses();
                }
            }

            public void mouseReleased(MouseEvent e) {
                checkPopupTrigger(e);
            }

            public void mousePressed(MouseEvent e) {
                checkPopupTrigger(e);
            }

            private void checkPopupTrigger(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectedCoursesPopupMenu.show(e.getComponent(),
                            e.getX(), e.getY());
                }
            }
        });
    }

    private void initDragonDrop() {
        final SchedulingSession session = getSession();
        possibleCoursesList.setTransferHandler(new CourseTransferHandler(session) {
            public int getSourceActions(JComponent c) { return COPY; }

            protected Collection<CourseDescriptor> getSelectedCourses() {
                int[] indices = possibleCoursesList.getSelectedIndices();
                return possibleCoursesModel.getCourses(indices);
            }

            protected void importCourses(List<CourseDescriptor> data) {
                if (data.isEmpty()) return;
                if (data.size() == 1) {
                    ensureCourseIsVisible(data.get(0));
                }
            }

            protected CourseAction getCourseImportAction() {
                return CourseAction.REMOVE;
            }

            protected CourseAction getCourseExportAction() {
                return CourseAction.ADD;
            }
        });
        selectedCoursesList.setTransferHandler(new CourseTransferHandler(session) {
            protected Collection<CourseDescriptor> getSelectedCourses() {
                return getSelectedPossibleCourses();
            }

            protected void exportDone(JComponent source, Transferable data, int action) {
                if (action == COPY || action == MOVE
                        || action == COPY_OR_MOVE) {
                    removeSelectedSelectedCourses();
                }
            }


            protected void importCourses(List<CourseDescriptor> data) {
                addCourses(data);
            }

            protected CourseAction getCourseImportAction() {
                return CourseAction.ADD;
            }

            protected CourseAction getCourseExportAction() {
                return CourseAction.REMOVE;
            }
        });
    }

    private void initButtons() {
        addButton.setAction(addAction);
        addButton.setText("Add");
        viewButton.setAction(viewAction);
        viewButton.setText("Times...");
        viewButton.setMnemonic('t');
        removeButton.setAction(removeCourseAction);
        removeButton.setText("Remove");
    }

    private void initPopupMenus() {
        if (profSearchType != null) {
            professorPopupMenu = new JMenu("Show courses taught by");
            professorPopupMenu.addMenuListener(new MenuListener() {
                private JMenuItem nullItem
                        = new JMenuItem("(No teacher listings found)");

                {
                    nullItem.setEnabled(false);
                }

                public void menuSelected(MenuEvent e) {
                    professorPopupMenu.removeAll();
                    SortedSet<Professor> profs = getSelectedProfessors(15);
                    if (profs.isEmpty()) {
                        professorPopupMenu.add(nullItem);
                    } else {
                        for (Professor prof : profs) {
                            final String name = prof.getName();
                            JMenuItem item = professorPopupMenu.add(name);
                            item.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    showCoursesByProfessor(name);
                                }
                            });
                        }
                    }
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuCanceled(MenuEvent e) {
                }
            });

            singleProfessorMenuItem = new JMenuItem();

            singleProfessorMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showCoursesByProfessor(lastSingleProfessorName);
                }
            });
        }

        addAction = new AddCourseAction();
        removeCourseAction = new RemoveCourseAction();
        viewAction = new ViewSectionsAction();

        possibleCoursesPopupMenu = new JPopupMenu();
        possibleCoursesPopupMenu.add(addAction);
        possibleCoursesPopupMenu.add(removeCourseAction);
        possibleCoursesPopupMenu.add(viewAction);
        possibleCoursesPopupMenu.addSeparator();

        if (profSearchType != null) {
            PopupMenuListener listener = new PopupMenuListener() {
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    assert profSearchType != null;

                    possibleCoursesPopupMenu.remove(profMenuIndex);

                    SortedSet<Professor> profs = getSelectedProfessors(2);
                    if (profs.size() > 1) {
                        possibleCoursesPopupMenu.insert(professorPopupMenu,
                                profMenuIndex);
                    } else {
                        possibleCoursesPopupMenu.insert(singleProfessorMenuItem,
                                profMenuIndex);
                        if (profs.size() == 1) {
                            Professor prof = profs.iterator().next();
                            String name = prof.getName();
                            singleProfessorMenuItem.setText(
                                    "Show courses taught by " + name);
                            singleProfessorMenuItem.setEnabled(true);
                            lastSingleProfessorName = name;
                        } else {
                            singleProfessorMenuItem.setText("Show courses taught "
                                    + "by (none)");
                            singleProfessorMenuItem.setEnabled(false);
                        }
                    }
                }

                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            };
            possibleCoursesPopupMenu.addPopupMenuListener(listener);
            possibleCoursesPopupMenu.add(professorPopupMenu);
            profMenuIndex = possibleCoursesPopupMenu.getComponentIndex(
                    professorPopupMenu);
        } else {
            profMenuIndex = -1;
        }


        possibleCoursesPopupMenu.add(new ShowAllCoursesAction());

        selectedCoursesPopupMenu = new JPopupMenu();
        selectedCoursesPopupMenu.add(removeCourseAction);
    }

    private void loadSearchBoxSettings() {
        String filter = PREFERENCES.get(PREF_SEARCH_TEXT, "");
        searchBox.setText(filter);
        String type = PREFERENCES.get(PREF_SEARCH_TYPE, null);
        if (type != null) {
            for (SearchType otherType : searchTypes.keySet()) {
                if (otherType.getIdentifier().equals(type)) {
                    searchBox.setSearchType(otherType);
                    break;
                }
            }
        }
    }

    /* Professor search */
    private boolean showCoursesByProfessor(String name) {
        if (profSearchType == null) return false;

        departmentModel.setSelectedDept(null);
        searchBox.setSearchType(profSearchType);
        searchBox.setText(name);
        return true;
    }

    private SortedSet<Professor> getSelectedProfessors(int max) {
        SortedSet<Professor> profs = new TreeSet<Professor>();
        int p = 0;
        Outer: for (CourseDescriptor cd : getSelectedPossibleCourses()) {
            for (Section section : cd.getActualCourse().getSections()) {
                for (ClassPeriod period : section.getPeriods()) {
                    Professor prof = period.getProfessor();
                    if (prof != null) {
                        profs.add(prof);
                    } else if (profs.size() >= max) {
                        // we got to the maximum
                        break Outer;
                    }
                    if (profs.size() == 0 && p > MAX_MATCHLESS_SEARCHED_ENTRIES) {
                        // this database might not have professors, so we don't
                        // want to go through the entire database every time the
                        // user right-clicks
                        break Outer;
                    }
                    p++;
                }
            }
        }
        return profs;
    }

    private void findProfessorSearchType() {
        int profKeySiblings = -1;
        SearchType profType = null;
        for (Map.Entry<SearchType, Set<SearchKey>> entry : searchTypes.entrySet()) {
            Set<SearchKey> keys = entry.getValue();
            if (keys.contains(CourseDocument.KEY_PROFESSOR)) {
                int numKeys = keys.size();
                if (profKeySiblings == -1 || numKeys < profKeySiblings) {
                    profType = entry.getKey();
                    profKeySiblings = numKeys;
                    if (numKeys == 1) break;
                }
            }
        }
        profSearchType = profType;
    }

    /* Manipulating course lists */
    private void addOrRemoveSelectedPossibleCourses() {
        boolean add = false;
        for (CourseDescriptor cd : getSelectedPossibleCourses()) {
            if (!selectedCoursesModel.containsCourse(cd)) {
                add = true;
                break;
            }
        }
        if (add) addSelectedPossibleCourses();
        else removeSelectedPossibleCourses();
    }
    private void addSelectedPossibleCourses() {
        addCourses(getSelectedPossibleCourses());
    }


    private void removeSelectedSelectedCourses() {
        selectedCoursesModel.removeCourses(selectedCoursesList.getSelectedRows());
    }

    private void removeSelectedPossibleCourses() {
        selectedCoursesModel.removeCourses(getSelectedPossibleCourses());
    }

    private void addCourses(List<CourseDescriptor> selected) {
        for (CourseDescriptor cd : selected) {
            if (couldBeAdded(cd)) addCourse(cd, false);
        }
        if (selected.size() == 1) {
            int index = selectedCoursesModel.getIndexOf(selected.get(0));
            ListSelectionModel selModel = selectedCoursesList.getSelectionModel();
            selModel.setSelectionInterval(index, index);
        }
    }

    private boolean couldBeAdded(CourseDescriptor cd) {
        return !selectedCoursesModel.containsCourse(cd)
                && conflictDetector.couldBeAdded(cd)
                && UITools.hasClassTime(cd.getActualCourse());
    }

    private void addCourse(CourseDescriptor cd, boolean extra) {
        selectedCoursesModel.addCourse(cd, extra);
    }

    private List<CourseDescriptor> getSelectedPossibleCourses() {
        int[] indices = possibleCoursesList.getSelectedIndices();
        List<CourseDescriptor> selected = possibleCoursesModel.getCourses(indices);
        return selected;
    }

    /* Synchronizing UI */
    private void updateDept() {
        possibleCoursesModel.setDept(departmentModel.getSelectedDept());
    }

    private void updateAddButtons() {
        int[] selindices = possibleCoursesList.getSelectedIndices();
        CourseDescriptor course;
        if (selindices.length == 1) {
            course = possibleCoursesModel.getCourseAt(selindices[0]);
        } else {
            course = null;
        }

//        updateText(selected);
        courseInfoPanel.setCourse(course);

        ListSelectionModel selModel = selectedCoursesList.getSelectionModel();
        if (course == null) {
            selModel.clearSelection();

        } else {
            if (courseViewerPanel != null && courseViewerWindow.isShowing()) {
                setCourseViewerCourse(course);
            }
            int index = selectedCoursesModel.getIndexOf(course);
            if (index == -1) {
                selModel.clearSelection();
            } else {
                selModel.setSelectionInterval(index, index);
                selectedCoursesList.scrollRectToVisible(
                        selectedCoursesList.getCellRect(index, 0, true));
            }
        }
    }

    private void ensureCourseIsVisible(CourseDescriptor course) {
        int index = possibleCoursesModel.indexOf(course);
        if (index == -1) {
            // make the right department visible
            Department dept = (Department) departmentBox.getSelectedItem();
            Department cdept = course.getDept();
            if (dept != null && !cdept.equals(dept)) {
                departmentModel.setSelectedItem(cdept);
            }
            index = possibleCoursesModel.indexOf(course);
            if (index == -1) {
                searchBox.setText("");
                index = possibleCoursesModel.indexOf(course);
                if (index == -1) {
//                    JOptionPane.showMessageDialog(CourseSelectionPanel.this,
//                            "The course you selected could not be shown. I'm so "
//                            + "very sorry, I hope it never happens again.");
                }
            }
        }
        if (index != -1) {
            possibleCoursesList.setSelectedIndex(index);
            possibleCoursesList.ensureIndexIsVisible(index);
        }
    }

    /* Subpanel functions */
    public void entering() {
        Collection<SelectedCourse> selectedCourses = getEngine().getSelectedCourses();
        selectedCoursesModel.setCourses(selectedCourses);
        updateAddButtons();
        possibleCoursesList.requestFocusInWindow();
    }

    public boolean canProgress() {
        return selectedCoursesModel.getRowCount() > 0;
    }

    public String getPrevButtonText() {
        return "Select Courses";
    }

    public String getNextButtonText() {
        return "Select Courses";
    }

    public void progressing() {
        SchedulerEngine engine = getEngine();
        engine.setSelectedCourses(selectedCoursesModel.getSelectedCourses());
    }

    public void disposeSubpanel() {
        // save the selected department
        Department selectedDept = departmentModel.getSelectedDept();
        if (selectedDept == null) {
            PREFERENCES.remove(PREF_SELECTED_DEPT);
        } else {
            PREFERENCES.put(PREF_SELECTED_DEPT, selectedDept.getAbbrev());
        }

        // save the search box state
        PREFERENCES.put(PREF_SEARCH_TEXT, searchBox.getText());
        SearchType selectedItem = searchBox.getSearchType();
        if (selectedItem != null) {
            PREFERENCES.put(PREF_SEARCH_TYPE, selectedItem.getIdentifier());
        }

        if (courseViewerWindow != null) {
            UITools.saveWindowPositionAndSize(courseViewerWindow, PREFERENCES,
                    KEY_COURSE_VIEWER);
            courseViewerWindow.dispose();
        }
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) { }
    }

    public String getPanelTitle() {
        return "Choose the courses that you want to take";
    }

    public String getInfoBarText() {
        return "When you're finished selecting courses, click Next to "
                + "choose the times you want class";
    }

    private void setCourseViewerCourse(CourseDescriptor course) {
        courseViewerPanel.setCourse(course);
        courseViewerWindow.setTitle(course.getActualCourse().getName());
    }

    private class CourseRenderer extends DefaultListCellRenderer {
        private ImageIcon emptyIcon = loadIcon("empty.png");
        private ImageIcon openIcon = loadIcon("open.png");
        private ImageIcon chosenIcon = loadIcon("chosen.png");
        private ImageIcon impossibleIcon = loadIcon("impossible.png");
//        private Icon nullIcon = new Icon() {
//            public void paintIcon(Component c, Graphics g, int x, int y) {
//            }
//
//            public int getIconWidth() {
//                return 16;
//            }
//
//            public int getIconHeight() {
//                return 16;
//            }
//        };

        private ImageIcon loadIcon(String name) {
            return new ImageIcon(CourseRenderer.class.getResource(name));
        }

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            PossibleCoursesListModel.CourseHolder holder
                    = (PossibleCoursesListModel.CourseHolder) value;
            CourseDescriptor cd = holder.getCourseDescriptor();

            String string = getSession().getUIPlugin().getCourseString(cd, StringFormatType.COURSE_SHORT);
            super.getListCellRendererComponent(list, string, index,
                    isSelected, cellHasFocus);

            boolean hasClass = UITools.hasClassTime(cd.getActualCourse());
            boolean chosen = selectedCoursesModel.containsCourse(cd);
            boolean impossible = !conflictDetector.couldBeAdded(cd);

            Icon icon;
            if (!hasClass) {
//                textColor = Color.MAGENTA;
                icon = emptyIcon;
                setForeground(Color.LIGHT_GRAY);
            } else if (chosen) {
//                textColor = Color.GREEN;
                icon = chosenIcon;
            } else if (impossible) {
                icon = impossibleIcon;
                setForeground(Color.LIGHT_GRAY);
            } else {
                icon = openIcon;
            }

            if (icon != null) setIcon(icon);

            return this;
        }
    }

    /* Actions */
    private class ShowAllCoursesAction extends AbstractAction {
        public ShowAllCoursesAction() {
            super("Show all courses");
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);
        }

        public void actionPerformed(ActionEvent e) {
            searchBox.setText("");
            departmentModel.setSelectedItem(null);
        }
    }

    private class AddCourseAction extends AbstractAction {
        public AddCourseAction() {
            super("Add course");
            putValue(MNEMONIC_KEY, KeyEvent.VK_A);

            ListSelectionModel model = possibleCoursesList.getSelectionModel();
            model.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateEnabled();
                }
            });
            selectedCoursesModel.addSelectedCoursesListener(
                    new SelectedCoursesListener() {
                public void coursesAdded(SelectedCoursesList courseList,
                        Collection<SelectedCoursesList.SelectedCourseHolder> added) {
                    updateEnabled();
                }

                public void coursesRemoved(SelectedCoursesList courseList,
                        Collection<SelectedCoursesList.SelectedCourseHolder> removed) {
                    updateEnabled();
                }

                public void coursesChanged(SelectedCoursesList courseList) {
                    updateEnabled();
                }

                public void courseStatusChanged(SelectedCoursesList courseList,
                        SelectedCoursesList.SelectedCourseHolder course,
                        boolean newExtra) {
                    updateEnabled();
                }
            });
            updateEnabled();
        }

        private void updateEnabled() {
            boolean good = false;
            for (CourseDescriptor cd : getSelectedPossibleCourses()) {
                if (couldBeAdded(cd)) {
                    good = true;
                    break;
                }
            }
            setEnabled(good);
        }

        public void actionPerformed(ActionEvent e) {
            addSelectedPossibleCourses();
        }
    }

    private class ViewSectionsAction extends AbstractAction {

        public ViewSectionsAction() {
            super("View class times");
            putValue(MNEMONIC_KEY, KeyEvent.VK_V);

            ListSelectionModel model = possibleCoursesList.getSelectionModel();
            model.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateEnabled();
                }
            });
            updateEnabled();
        }

        private void updateEnabled() {
            List<CourseDescriptor> courses = getSelectedPossibleCourses();
            boolean okay = false;
            Outer: for (CourseDescriptor cd : courses) {
                for (Section section : cd.getActualCourse().getSections()) {
                    for (ClassPeriod period : section.getPeriods()) {
                        if (period.getTimePeriod().getPeriod().getElapsedMinutes() > 0) {
                            okay = true;
                            break Outer;
                        }
                    }
                }
            }
            setEnabled(!courses.isEmpty() && okay);
        }

        public void actionPerformed(ActionEvent e) {
            if (courseViewerPanel == null) {
                courseViewerPanel = new CourseTimeViewerPanel();
                courseViewerPanel.setSession(getSession());
                courseViewerWindow = new MonitoredJFrame("Course Preview");
                courseViewerWindow.getContentPane().add(courseViewerPanel);
                if (UITools.loadWindowPosition(courseViewerWindow, PREFERENCES,
                        KEY_COURSE_VIEWER)) {
                    UITools.fixWindowLocation(courseViewerWindow);
                } else {
                    Window window = (Window) SwingUtilities.getAncestorOfClass(
                            Window.class, CourseSelectionPanel.this);
                    courseViewerWindow.setLocationRelativeTo(window);
                }
                SchedulerUIPlugin plugin = getSession().getUIPlugin();
                if (!UITools.loadWindowSize(courseViewerWindow, PREFERENCES,
                        KEY_COURSE_VIEWER)) {
                    courseViewerWindow.setSize(plugin.getDefaultWindowSize(WindowType.COURSE_PREVIEW));
                }
                UITools.fixWindowSize(courseViewerWindow);
                courseViewerWindow.setIconImage(plugin.getWindowIcon(WindowType.COURSE_PREVIEW));
            }
            int[] selection = possibleCoursesList.getSelectedIndices();
            List<CourseDescriptor> courses = possibleCoursesModel.getCourses(selection);
            if (courses.size() == 1) {
                CourseDescriptor course = courses.iterator().next();
                setCourseViewerCourse(course);
                courseViewerWindow.setVisible(true);
            }
        }
    }

    private class RemoveCourseAction extends AbstractAction {
        public RemoveCourseAction() {
            super("Remove course");
            putValue(MNEMONIC_KEY, KeyEvent.VK_R);

            ListSelectionListener l = new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateEnabled();
                }
            };
            selectedCoursesList.getSelectionModel().addListSelectionListener(l);
            possibleCoursesList.getSelectionModel().addListSelectionListener(l);
            FocusListener fl = new FocusListener() {
                public void focusGained(FocusEvent e) {
                    updateEnabled();
                }

                public void focusLost(FocusEvent e) {
                    updateEnabled();
                }
            };
            selectedCoursesList.addFocusListener(fl);
            possibleCoursesList.addFocusListener(fl);
            updateEnabled();
        }

        private void updateEnabled() {
            if (selectedCoursesList.isFocusOwner()) {
                setEnabled(selectedCoursesList.getSelectedRowCount() > 0);
            } else {
                int[] indices = possibleCoursesList.getSelectedIndices();
                if (indices.length == 0) setEnabled(false);

                boolean toenable = false;

                List<CourseDescriptor> courses
                        = possibleCoursesModel.getCourses(indices);
                for (CourseDescriptor cd : courses) {
                    if (selectedCoursesModel.containsCourse(cd)) {
                        toenable = true;
                        break;
                    }
                }
                setEnabled(toenable);
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (selectedCoursesList.isFocusOwner()) {
                removeSelectedSelectedCourses();
            } else {
                removeSelectedPossibleCourses();
            }
        }
    }
}
