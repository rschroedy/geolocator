/**
 * 
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 * 
 * @author Wei Zhang,  Language Technology Institute, School of Computer Science, Carnegie-Mellon University.
 * email: wei.zhang@cs.cmu.edu
 * 
 */
package edu.cmu.geoparser.ui.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.cybozu.labs.langdetect.LangDetectException;

import edu.cmu.geoparser.Disambiguation.ContextDisamb;
import edu.cmu.geoparser.io.GetReader;
import edu.cmu.geoparser.model.Tweet;
import edu.cmu.geoparser.nlp.languagedetector.LangDetector;
import edu.cmu.geoparser.nlp.ner.FeatureExtractor.FeatureGenerator;
import edu.cmu.geoparser.parser.english.EnglishParser;
import edu.cmu.geoparser.parser.spanish.SpanishParser;
import edu.cmu.geoparser.resource.trie.IndexSupportedTrie;

/**
 * This demo shows the on-the-fly tagging of the sample text or JSON
 * 
 */
public class CmdInputParser {

  public static void main(String argv[]) throws IOException {

    boolean misspell = argv[0].equals("mis")?true:false;
    String trieDic = argv[1];
    String gazIndex = argv[2];
    /**
     * Build the in-memory version of the gazetteer as a trie tree. 
     * Using allCountries.txt will be quite slow, the memory usage is nearly 1.2G. However, it includes all types of locations, including
     * country, state, city, building, town, parks, lakes, oceans,...
     * However, using cities1000.txt (downloadable in Geonames free gazetteer site also) will give you only the cities. The loading will pretty fast, though.
     * 
     */
    IndexSupportedTrie topotrie = new IndexSupportedTrie(trieDic,gazIndex, true, false);

    /**
     * This is the main construction function for the English parser. The Spanish parser has the same form.
     */
    EnglishParser enparser = new EnglishParser("res/", topotrie, misspell);

    /**
     * Initialize a context disambiguation class c.
     */
    ContextDisamb c = new ContextDisamb();
    
    /**
     * Build a language detector. However, here we don't use it.
     * See the class LangDetector for instructions. 
     */
    LangDetector lang = new LangDetector("res/langdetect.profile");

    String text = null;
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "utf-8"));
    System.out.print(">");

    while ((text = br.readLine()) != null) {

      if (text.length() == 0)
        continue;

      List<String> match = null;
      /**
       * This is a tool for parsing tweets. So the text is wrapped into a Tweet. If you want to parser other types of text, just wrap it into a tweet,
       * set the tweet text field with your text, and leave the other fields alone.
       */
      Tweet t = new Tweet(text);

      /**
       * Generate the parsed matches.
       */
      // Parse topo
      match = enparser.parse(t);

      if (match == null) {
        System.out.println("No toponyms in text.");
        continue;
      } else if (match.size() == 0) {
        System.out.println("No toponyms in text.");
        continue;
      } else { // if matches are found:
        System.out.println("The locations found :");
        System.out.println(match);

        /**
         * Then, we disambiguate the parsed location.
         */

        /**
         * The parsing output will give you parsed results in the form of tp{XXX}tp, or TP{XXX}TP.
         * The next lines are for stripping the first and last three characters. 
         */
        HashSet<String> reducedmatch = new HashSet<String>();
        for (String s : match)
          reducedmatch.add(s.substring(3, s.length() - 3));

        /**
         * Here it will generate a list of topos for the reduced matches.
         * The data passing into the returnBestTopo can be a bunch of locations. Then, the returned Hashmap 
         * will give you a map between location, and it's string array storing the longitude, latitude, and country state info.
         */
        // Disambiguate topo
        HashMap<String, String[]> result = c.returnBestTopo(topotrie, reducedmatch);

        if (result == null) {
          System.out.println("No GPS for any location is found.");
        } else {
          System.out.println("The grounded location(s) are:");
          for (String topo : result.keySet())
            System.out.println(topo + ": " + result.get(topo)[2] + " " + result.get(topo)[0] + " "
                    + result.get(topo)[1]);
        }
      }
      System.out.print(">");
    }// end of while

  }
}
