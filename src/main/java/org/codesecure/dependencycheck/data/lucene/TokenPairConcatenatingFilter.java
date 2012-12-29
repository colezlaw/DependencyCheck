package org.codesecure.dependencycheck.data.lucene;
/*
 * This file is part of DependencyCheck.
 *
 * DependencyCheck is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * DependencyCheck is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * DependencyCheck. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */

import java.io.IOException;
import java.util.LinkedList;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * <p>Takes a TokenStream and adds additional tokens by concatenating pairs of words.</p>
 * <p><b>Example:</b> "Spring Framework Core" -> "Spring SpringFramework Framework FrameworkCore Core".</p>
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public final class TokenPairConcatenatingFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private String previousWord = null;
    private LinkedList<String> words = null;

    /**
     * Consructs a new TokenPairConcatenatingFilter
     * @param stream the TokenStream that this filter will process
     */
    public TokenPairConcatenatingFilter(TokenStream stream) {
        super(stream);
        words = new LinkedList<String>();
    }

    /**
     * Increments the underlying TokenStream and sets CharTermAtttributes to
     * construct an expanded set of tokens by concatenting tokens with the
     * previous token.
     *
     * @return whether or not we have hit the end of the TokenStream
     * @throws IOException is thrown when an IOException occurs
     */
    @Override
    public boolean incrementToken() throws IOException {

        //collect all the terms into the words collaction
        while (input.incrementToken()) {
            String word = new String(termAtt.buffer(), 0, termAtt.length());
            words.add(word);
        }

        //if we have a previousTerm - write it out as its own token concatonated
        // with the current word (if one is available).
        if (previousWord != null && words.size() > 0) {
            String word = words.getFirst();
            clearAttributes();
            termAtt.append(previousWord).append(word);
            posIncAtt.setPositionIncrement(0);
            previousWord = null;
            return true;
        }
        //if we have words, write it out as a single token
        if (words.size() > 0) {
            String word = words.removeFirst();
            clearAttributes();
            termAtt.append(word);
            previousWord = word;
            return true;
        }
        return false;
    }

    /**
     * <p>Resets the Filter and clears any internal state data that may
     * have been left-over from previous uses of the Filter.</p>
     * <p><b>If this Filter is re-used this method must be called between uses.</b></p>
     */
    public void clear() {
        previousWord = null;
        words.clear();
    }
}
