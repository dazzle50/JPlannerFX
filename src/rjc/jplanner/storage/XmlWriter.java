/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.storage;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.xml.stream.XMLStreamWriter;

/*************************************************************************************************/
/******** Simple XML writer for generated XML documents preserving whitespace characters *********/
/*************************************************************************************************/

/**
 * Writes small, well-formed XML documents directly to a {@link Writer}.
 * <p>
 * This class is intended for generated XML where the caller controls element and attribute names,
 * but attribute values may contain significant whitespace. Attribute carriage returns, line feeds,
 * and tabs are always written as character references so they survive XML parser attribute
 * normalisation (unlike {@link XMLStreamWriter}).
 * <p>
 * The writer is deliberately small and stateful. Attributes must be written immediately after
 * {@link #startElement(String)}, before text or child elements are written. Only one root element
 * is permitted; a second top-level {@link #startElement(String)} call is rejected.
 * <p>
 * Closing this object verifies that all elements have been ended, but does not close the underlying
 * {@link Writer}. The caller owns the destination writer.
 * <p>
 * This class is not thread-safe. Instances must be confined to a single thread.
 */
public final class XmlWriter implements AutoCloseable
{
  private final Writer        m_writer;
  private final Deque<String> m_elements = new ArrayDeque<>();
  private boolean             m_startElementOpen;
  private boolean             m_rootStarted;
  private boolean             m_closed;

  /***************************************** constructor *****************************************/
  /**
   * Creates an XML writer around the supplied destination.
   *
   * @param writer the destination writer
   */
  public XmlWriter( Writer writer )
  {
    m_writer = Objects.requireNonNull( writer, "writer" );
  }

  /***************************************** startElement ****************************************/
  /**
   * Starts an element.
   *
   * @param name the element name
   * @return this writer
   * @throws IOException if writing fails
   * @throws IllegalStateException if a second root element is started
   */
  public XmlWriter startElement( String name ) throws IOException
  {
    requireOpen();
    requireName( name );

    // enforce a single document (root) element
    if ( m_elements.isEmpty() )
    {
      if ( m_rootStarted )
        throw new IllegalStateException( "document already has a root element" );
      m_rootStarted = true;
    }

    closeStartElementIfNeeded();

    m_writer.write( '<' );
    m_writer.write( name );
    m_elements.push( name );
    m_startElementOpen = true;

    return this;
  }

  /****************************************** attribute ******************************************/
  /**
   * Writes an attribute on the current open start element.
   *
   * @param name the attribute name
   * @param value the attribute value; {@code null} is written as an empty string
   * @return this writer
   * @throws IOException if writing fails
   * @throws IllegalStateException if no start element is open
   */
  public XmlWriter attribute( String name, Object value ) throws IOException
  {
    requireOpen();
    requireName( name );

    if ( !m_startElementOpen )
      throw new IllegalStateException( "attributes must be written immediately after startElement" );

    m_writer.write( ' ' );
    m_writer.write( name );
    m_writer.write( "=\"" );
    writeEscaped( value == null ? "" : value.toString(), XmlWriter::attributeReplacement );
    m_writer.write( '"' );

    return this;
  }

  /********************************************* text ********************************************/
  /**
   * Writes text content.
   *
   * @param text the text to write; {@code null} writes nothing
   * @return this writer
   * @throws IOException if writing fails
   * @throws IllegalStateException if no element is currently open
   */
  public XmlWriter text( String text ) throws IOException
  {
    requireOpen();

    // text is only meaningful inside an element, never at document level
    if ( m_elements.isEmpty() )
      throw new IllegalStateException( "text must be written inside an element" );

    if ( text == null || text.isEmpty() )
      return this;

    closeStartElementIfNeeded();
    writeEscaped( text, XmlWriter::textReplacement );

    return this;
  }

  /****************************************** endElement *****************************************/
  /**
   * Ends the current element.
   *
   * @return this writer
   * @throws IOException if writing fails
   */
  public XmlWriter endElement() throws IOException
  {
    requireOpen();

    if ( m_elements.isEmpty() )
      throw new IllegalStateException( "no element is open" );

    String name = m_elements.pop();
    if ( m_startElementOpen )
    {
      m_writer.write( "/>" );
      m_startElementOpen = false;
      return this;
    }

    m_writer.write( "</" );
    m_writer.write( name );
    m_writer.write( '>' );

    return this;
  }

  /***************************************** endDocument *****************************************/
  /**
   * Ends all open elements.
   *
   * @return this writer
   * @throws IOException if writing fails
   */
  public XmlWriter endDocument() throws IOException
  {
    requireOpen();

    while ( !m_elements.isEmpty() )
      endElement();

    return this;
  }

  /******************************************** flush ********************************************/
  /**
   * Flushes the underlying writer.
   * <p>
   * If a start element is currently open, this closes its start tag first (as writing text or a
   * child element would), so no further attributes can be added to it afterwards.
   *
   * @throws IOException if flushing fails
   */
  public void flush() throws IOException
  {
    requireOpen();
    closeStartElementIfNeeded();
    m_writer.flush();
  }

  /******************************************** close ********************************************/
  /**
   * Verifies that the document is complete.
   *
   * @throws IllegalStateException if an element remains unclosed
   */
  @Override
  public void close()
  {
    if ( m_closed )
      return;

    if ( !m_elements.isEmpty() )
      throw new IllegalStateException( "unclosed element: " + m_elements.peek() );

    m_closed = true;
  }

  /********************************** closeStartElementIfNeeded **********************************/
  private void closeStartElementIfNeeded() throws IOException
  {
    if ( !m_startElementOpen )
      return;

    m_writer.write( '>' );
    m_startElementOpen = false;
  }

  /***************************************** writeEscaped ****************************************/
  private void writeEscaped( String value, IntFunction<String> escaper ) throws IOException
  {
    int start = 0;
    int i = 0;

    // walk by code point, not char, so surrogate pairs are treated as one unit
    while ( i < value.length() )
    {
      int codePoint = value.codePointAt( i );
      int charCount = Character.charCount( codePoint );
      String replacement = escaper.apply( codePoint );

      if ( replacement == null )
      {
        requireLegalCodePoint( codePoint );
        i += charCount;
        continue;
      }

      // Writer.write with a zero length is a documented no-op, so no need to guard it here
      m_writer.write( value, start, i - start );
      m_writer.write( replacement );
      i += charCount;
      start = i;
    }

    m_writer.write( value, start, value.length() - start );
  }

  /************************************* attributeReplacement ************************************/
  private static String attributeReplacement( int codePoint )
  {
    return switch ( codePoint )
    {
      case '&' -> "&amp;";
      case '<' -> "&lt;";
      case '"' -> "&quot;";
      case '\r' -> "&#xD;";
      case '\n' -> "&#xA;";
      case '\t' -> "&#x9;";
      default -> null;
    };
  }

  /*************************************** textReplacement ***************************************/
  private static String textReplacement( int codePoint )
  {
    return switch ( codePoint )
    {
      case '&' -> "&amp;";
      case '<' -> "&lt;";
      case '>' -> "&gt;";
      case '\r' -> "&#xD;";
      default -> null;
    };
  }

  /***************************************** requireName *****************************************/
  private static void requireName( String name )
  {
    if ( name == null || name.isEmpty() )
      throw new IllegalArgumentException( "XML name must not be null or empty" );
  }

  /************************************ requireLegalCodePoint ************************************/
  private static void requireLegalCodePoint( int codePoint )
  {
    // XML 1.0 Char production excludes most control characters, lone surrogates and the
    // U+FFFE / U+FFFF noncharacters; supplementary-plane noncharacters are legal per spec
    boolean controlCharacter = codePoint < 0x20 && codePoint != '\t' && codePoint != '\n' && codePoint != '\r';

    // codePointAt only yields a supplementary value (>= 0x10000) for an already-valid pair,
    // so a lone/unpaired surrogate can only ever appear as a BMP code point; casting a
    // supplementary code point to char without this guard truncates it and can alias into
    // the surrogate range, wrongly rejecting valid characters such as U+1D800
    boolean loneSurrogate = Character.isBmpCodePoint( codePoint ) && Character.isSurrogate( (char) codePoint );

    boolean nonCharacter = codePoint == 0xFFFE || codePoint == 0xFFFF;

    if ( controlCharacter || loneSurrogate || nonCharacter )
      throw new IllegalArgumentException(
          "illegal XML character: U+" + Integer.toHexString( codePoint ).toUpperCase() );
  }

  /***************************************** requireOpen *****************************************/
  private void requireOpen()
  {
    if ( m_closed )
      throw new IllegalStateException( "writer is closed" );
  }

}