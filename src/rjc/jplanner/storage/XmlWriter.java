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
 * {@link #startElement(String)}, before text or child elements are written.
 * <p>
 * Closing this object verifies that all elements have been ended, but does not close the underlying
 * {@link Writer}. The caller owns the destination writer.
 */
public final class XmlWriter implements AutoCloseable
{
  private final Writer        m_writer;
  private final Deque<String> m_elements = new ArrayDeque<>();
  private boolean             m_startElementOpen;
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

  /**************************************** startElement *****************************************/
  /**
   * Starts an element.
   *
   * @param name the element name
   * @return this writer
   * @throws IOException if writing fails
   */
  public XmlWriter startElement( String name ) throws IOException
  {
    requireOpen();
    requireName( name );
    closeStartElementIfNeeded();

    m_writer.write( '<' );
    m_writer.write( name );
    m_elements.push( name );
    m_startElementOpen = true;

    return this;
  }

  /***************************************** attribute *******************************************/
  /**
   * Writes an attribute on the current open start element.
   *
   * @param name the attribute name
   * @param value the attribute value; {@code null} is written as an empty string
   * @return this writer
   * @throws IOException if writing fails
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
    writeAttributeValue( value == null ? "" : value.toString() );
    m_writer.write( '"' );

    return this;
  }

  /******************************************* text **********************************************/
  /**
   * Writes text content.
   *
   * @param text the text to write; {@code null} writes nothing
   * @return this writer
   * @throws IOException if writing fails
   */
  public XmlWriter text( String text ) throws IOException
  {
    requireOpen();

    if ( text == null || text.isEmpty() )
      return this;

    closeStartElementIfNeeded();
    writeTextValue( text );

    return this;
  }

  /**************************************** endElement *******************************************/
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

  /****************************************** flush **********************************************/
  /**
   * Flushes the underlying writer.
   *
   * @throws IOException if flushing fails
   */
  public void flush() throws IOException
  {
    requireOpen();
    closeStartElementIfNeeded();
    m_writer.flush();
  }

  /****************************************** close **********************************************/
  /**
   * Verifies that the document is complete.
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

  /********************************** writeAttributeValue ****************************************/
  private void writeAttributeValue( String value ) throws IOException
  {
    int start = 0;

    for ( int i = 0; i < value.length(); i++ )
    {
      char c = value.charAt( i );
      String replacement = attributeReplacement( c );

      if ( replacement == null )
      {
        requireLegalXmlCharacter( c );
        continue;
      }

      writeRun( value, start, i );
      m_writer.write( replacement );
      start = i + 1;
    }

    writeRun( value, start, value.length() );
  }

  /************************************* writeTextValue ******************************************/
  private void writeTextValue( String value ) throws IOException
  {
    int start = 0;

    for ( int i = 0; i < value.length(); i++ )
    {
      char c = value.charAt( i );
      String replacement = textReplacement( c );

      if ( replacement == null )
      {
        requireLegalXmlCharacter( c );
        continue;
      }

      writeRun( value, start, i );
      m_writer.write( replacement );
      start = i + 1;
    }

    writeRun( value, start, value.length() );
  }

  /*************************************** writeRun **********************************************/
  private void writeRun( String value, int start, int end ) throws IOException
  {
    if ( end > start )
      m_writer.write( value, start, end - start );
  }

  private static String attributeReplacement( char c )
  {
    return switch ( c )
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

  /************************************** textReplacement ****************************************/
  private static String textReplacement( char c )
  {
    return switch ( c )
    {
      case '&' -> "&amp;";
      case '<' -> "&lt;";
      case '>' -> "&gt;";
      case '\r' -> "&#xD;";
      default -> null;
    };
  }

  /*************************************** requireName *******************************************/
  private static void requireName( String name )
  {
    if ( name == null || name.isEmpty() )
      throw new IllegalArgumentException( "XML name must not be null or empty" );
  }

  /*********************************** requireLegalXmlCharacter **********************************/
  private static void requireLegalXmlCharacter( char c )
  {
    if ( c < 0x20 && c != '\t' && c != '\n' && c != '\r' )
      throw new IllegalArgumentException( "illegal XML character: 0x" + Integer.toHexString( c ) );
  }

  /**************************************** requireOpen ******************************************/
  private void requireOpen()
  {
    if ( m_closed )
      throw new IllegalStateException( "writer is closed" );
  }

}