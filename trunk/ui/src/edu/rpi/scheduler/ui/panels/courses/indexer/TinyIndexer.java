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

package edu.rpi.scheduler.ui.panels.courses.indexer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

public class TinyIndexer {
    private static final int MAP_SIZE = (int) ((26*26 + 26 + 10) * (1/0.75));

    private HashMap<String, Map<SearchKey, Set<IndexableDocument>>> documents
            = new HashMap<String, Map<SearchKey, Set<IndexableDocument>>>(MAP_SIZE, 0.75f);
    private Set<IndexableDocument> allMatches
            = new HashSet<IndexableDocument>(1000);

    public void registerObject(IndexableDocument document) {
        Map<SearchKey,Set<String>> strings = document.getStrings();
        for (Map.Entry<SearchKey,Set<String>> entry : strings.entrySet()) {
            for (String string : entry.getValue()) {
                for (int ci = 0; ci < string.length() - 1; ci++) {
                    char a = string.charAt(ci);
                    char b = string.charAt(ci + 1);
                    registerObject(document, entry.getKey(), a, b);
                    if (ci == 0) registerObject(document, entry.getKey(), a);
                    registerObject(document, entry.getKey(), b);
                }
            }
        }
        allMatches.add(document);
    }

    private void registerObject(IndexableDocument document, SearchKey type, char a) {
        registerObject(document, type, new String(new char[] { a }));
    }

    private void registerObject(IndexableDocument document, SearchKey type, char a, char b) {
        registerObject(document, type, new String(new char[] { a, b }));
    }

    private void registerObject(IndexableDocument document, SearchKey type, String os) {
        assert os.length() == 1 || os.length() == 2;
        String s = os.toLowerCase();
        Map<SearchKey, Set<IndexableDocument>> typeMap = documents.get(s);
        if (typeMap == null) {
            typeMap = new HashMap<SearchKey, Set<IndexableDocument>>();
            documents.put(s, typeMap);
        }
        Set<IndexableDocument> family = typeMap.get(type);
        if (family == null) {
            family = new HashSet<IndexableDocument>();
            typeMap.put(type, family);
        }
        family.add(document);
    }

    public Set<IndexableDocument> getMatches(String text, Set<SearchKey> keys) {
        if (text.length() == 0) return allMatches;

        Set<IndexableDocument> set = new HashSet<IndexableDocument>(documents.size() / 10);
        Set<IndexableDocument> temp = new HashSet<IndexableDocument>(documents.size() / 10);
        addMatches(getPrefix(text), keys, set);
        if (text.length() > 2) {
            retainMatches(text.substring(1), keys, set, temp);
            for (Iterator<IndexableDocument> it = set.iterator(); it.hasNext();) {
                if (!documentMatches(it.next(), keys, text)) it.remove();
            }
        }
        return set;
    }

    private void retainMatches(String text, Set<SearchKey> keys,
            Set<IndexableDocument> set, Set<IndexableDocument> temp) {
        temp.clear();
        String tiny = getPrefix(text);
        addMatches(tiny, keys, temp);
        set.retainAll(temp);
        // we use >2 because we can skip the last letter since it obviously
        // matches
        if (text.length() > 2) retainMatches(text.substring(1), keys, set, temp);
    }

    private String getPrefix(String text) {
        String tiny = (text.length() > 2 ? text.substring(0, 2) : text);
        return tiny;
    }

    private void addMatches(String fragment, Set<SearchKey> keys, Set<IndexableDocument> docs) {
        int length = fragment.length();
        if (length == 0) {
            return;
        } else if (length == 1 || length == 2) {
            Map<SearchKey, Set<IndexableDocument>> matches = documents.get(fragment);
            if (matches == null) return;
            if (keys == null) {
                for (Set<IndexableDocument> set : matches.values()) {
                  docs.addAll(set);
                }
            } else {
                for (SearchKey key : keys) {
                    Set<IndexableDocument> matchingDocs = matches.get(key);
                    if (matchingDocs != null) docs.addAll(matchingDocs);
                }
            }
        } else {
            throw new IllegalArgumentException("illegal string " + fragment);
        }
    }

//    private Set<IndexableDocument> oldgetMatches(String text, Set<?> keys) {
//        Set<IndexableDocument> set = new HashSet<IndexableDocument>(documents.size() / 10);
//        String original = text.toLowerCase().trim();
//        addMatchesExpensively(original, original, keys, set);
//        return set;
//    }
//
//    private void addMatchesExpensively(String original, String fragment,
//            Set<?> keys, Set<IndexableDocument> set) {
//        int length = fragment.length();
//        if (length == 0) {
//            return;
//        } else if (length == 1 || length == 2) {
//            Map<Object, Set<IndexableDocument>> matches = documents.get(fragment);
//            if (matches == null) return;
//            for (Object o : keys) {
//
//            }
//            Documents: for (IndexableDocument document : family) {
//                for (Set<String> strings : document.getStrings().values()) {
//                    for (String s : strings) {
//                        if (s.toLowerCase().indexOf(original) != -1) {
//                            set.add(document);
//                            continue Documents;
//                        }
//                    }
//                }
//            }
//        } else {
//            addMatches(fragment.substring(0, 2), set);
//            addMatches(fragment.substring(2), set);
//        }
//    }

    private boolean documentMatches(IndexableDocument doc, Set<SearchKey> keys,
            String original) {
        Map<SearchKey,Set<String>> strings = doc.getStrings();
        if (keys == null) {
            for (Set<String> set : strings.values()) {
              if (stringMatchesAny(set, original)) return true;
            }
        } else {
            for (SearchKey key : keys) {
                Set<String> set = strings.get(key);
                if (set == null) continue;
                if (stringMatchesAny(set, original)) return true;
            }
        }
        return false;
    }

    private boolean stringMatchesAny(Set<String> set, String original) {
        for (String s : set) {
            if (s.toLowerCase().indexOf(original) != -1) return true;
        }
        return false;
    }
}
