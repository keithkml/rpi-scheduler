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

package edu.rpi.dbparser;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.Date;
import java.net.URL;

public class Parse {
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: Parse <file> [output]");
            System.err.println("  (output defaults to console output)");
            System.exit(1);
        }
        String infile = args[0];
        Parser parser = new Parser();
        Reader reader;
        if (new File(infile).exists()) {
            try {
                reader = new FileReader(infile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
                return;
            }
        } else {
            try {
                reader = new InputStreamReader(new URL(infile).openStream());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
                return;
            }
        }

        try {
            parser.parse(reader);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
        String outfile = args[1];
        OutputStream os;
        if (args.length >= 2) {
            String backupfilename = outfile + ".old";
            try {
                File bf = new File(backupfilename);
                bf.delete();
                new File(outfile).renameTo(bf);
            } catch (Exception e) {
                System.err.println("Can't copy file " + outfile + " to " + backupfilename + ":");
            }
            try {
                os = new FileOutputStream(outfile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(2);
                return;
            }
        } else {
            os = System.out;
        }

        Element root = new Element("schedb");
        root.setAttribute("generated", new Date().toString());
        root.setAttribute("minutes-per-block", "30");
        parser.writeExternalMod(root);
        Document doc = new Document(root);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        try {
            out.output(doc, os);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
            return;
        }
    }
}
