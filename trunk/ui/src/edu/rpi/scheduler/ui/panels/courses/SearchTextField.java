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

import edu.rpi.scheduler.CopyOnWriteArrayList;
import edu.rpi.scheduler.ui.panels.courses.indexer.SearchType;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchTextField extends JTextField {
    private JButton button;
    private JPopupMenu searchTypeMenu = new JPopupMenu("Search type");

    private SearchType searchType = null;
    private List<SearchType> searchTypes;
    private Map<SearchType,JCheckBoxMenuItem> items
            = new HashMap<SearchType, JCheckBoxMenuItem>();
    private CopyOnWriteArrayList<SearchTextFieldListener> listeners
            = new CopyOnWriteArrayList<SearchTextFieldListener>();

    public List<SearchType> getSearchTypes() {
        return Collections.unmodifiableList(new ArrayList<SearchType>(searchTypes));
    }

    public void setSearchTypes(Collection<SearchType> searchTypes) {
        this.searchTypes = new ArrayList<SearchType>(searchTypes);

        items.clear();
        searchTypeMenu.removeAll();
        for (SearchType type : this.searchTypes) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new SetSearchTypeAction(type));
            searchTypeMenu.add(item);
            items.put(type, item);
        }
    }

    public SearchType getSearchType() { return searchType; }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
        fireSearchTypeChanged(searchType);
    }

    private void fireSearchTypeChanged(SearchType searchType) {
        for (SearchTextFieldListener listener : listeners) {
            listener.searchTypeChanged(this, searchType);
        }
    }

    private void prepareMenu() {
        for (Map.Entry<SearchType, JCheckBoxMenuItem> entry : items.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(searchType));
        }
    }

    public void init() {
        setBorder(new CompoundBorder(getBorder(), new EmptyBorder(0, 27, 0, 0)));

        URL resource = SearchTextField.class.getResource("search.png");
        ImageIcon icon;
        if (resource != null) icon = new ImageIcon(resource);
        else icon = null;
        button = new JButton(icon);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchTypeMenu.show(SearchTextField.this, 6, 20);
            }
        });
        button.setUI(new BasicButtonUI());
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        add(button);
        button.setLocation(6, 4);
        button.setSize(16, 16);

        searchTypeMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                prepareMenu();
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        //        clearSearchBoxAction = new ClearSearchBoxAction();

        getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                fireSearchTextChangedEvent(getText());

//                clearSearchBoxAction.setEnabled(!searchText.equals(""));
            }
        });
//        searchBox.setToolTipText(finderPanel.getToolTipText());


//        Set<SearchType> types = searchTypes.keySet();
//        SearchType[] things = types.toArray(new SearchType[types.size()]);
//        searchTypeBox.setModel(new DefaultComboBoxModel(things));
//        searchTypeBox.setRenderer(new DefaultListCellRenderer() {
//            public Component getListCellRendererComponent(JList list, Object value,
//                    int index, boolean isSelected, boolean cellHasFocus) {
//                SearchType st = (SearchType) value;
//                String name = st == null ? null : st.getDisplayName();
//                return super.getListCellRendererComponent(list, name, index,
//                        isSelected, cellHasFocus);
//            }
//        });
//        searchTypeBox.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent event) {
//                if (event.getStateChange() == ItemEvent.SELECTED) {
//                    Set<SearchKey> keys = searchTypes.get(event.getItem());
//                    possibleCoursesModel.setFilterKeys(keys);
//                }
//            }
//        });

//        xButton.setAction(clearSearchBoxAction);
//        xButton.setText("X");
    }

    public void setSearchText(String text) {
        setText(text);
        fireSearchTextChangedEvent(text);
    }

    public String getSearchText() {
        return getText();
    }

    private void fireSearchTextChangedEvent(String text) {
        for (SearchTextFieldListener listener : listeners) {
            listener.searchStringChanged(this, text);
        }
    }

    public void addSearchTextFieldListener(SearchTextFieldListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeSearchTextFieldListener(SearchTextFieldListener l) {
        listeners.remove(l);
    }

    private class SetSearchTypeAction extends AbstractAction {
        private SearchType key;

        public SetSearchTypeAction(SearchType key) {
            super(key.getDisplayName());

            this.key = key;
        }

        public void actionPerformed(ActionEvent e) {
            setSearchType(key);
            SearchTextField.this.requestFocusInWindow();
        }
    }/*
    private class ClearSearchBoxAction extends AbstractAction {
        public ClearSearchBoxAction() {
            super("Clear");

            putValue(SHORT_DESCRIPTION, "Clear the Quick Find box");
            setEnabled(!searchBox.getText().equals(""));
        }

        public void actionPerformed(ActionEvent e) {
            searchBox.setText("");
            searchBox.requestFocusInWindow();
        }
    }*/
}
