/**************************************************************************
 *  Copyright (C) 2018 by Richard Crook                                   *
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

package rjc.jplanner.gui;

import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.text.Text;

/*************************************************************************************************/
/*********************************** Enhanced JavaFX TextField ***********************************/
/*************************************************************************************************/

public class XTextField extends TextField
{
  private Pattern          m_allowed;              // pattern defining text allowed to be entered
  private double           m_minWidth;             // minimum width for editor in pixels
  private double           m_maxWidth;             // maximum width for editor in pixels
  private ButtonType       m_buttonType;           // button type, null means no button
  private Canvas           m_button;               // canvas to show button
  private int              m_caretPos        = -1; // set to >= 0 to position caret after setting text

  private static final int BUTTONS_WIDTH_MAX = 16;
  private static final int BUTTONS_PADDING   = 2;

  public enum ButtonType
  {
    DOWN, UP_DOWN
  }

  /**************************************** constructor ******************************************/
  public XTextField()
  {
    // create enhanced text field control 
    textProperty().addListener( ( observable, oldText, newText ) ->
    {
      // if min & max width set, increase editor width if needed to show whole text
      if ( m_minWidth > 0.0 && m_maxWidth > m_minWidth )
      {
        Text text = new Text( newText );
        double width = text.getLayoutBounds().getWidth() + getPadding().getLeft() + getPadding().getRight()
            + BUTTONS_PADDING;
        if ( width < m_minWidth )
          width = m_minWidth;
        if ( width > m_maxWidth )
          width = m_maxWidth;
        if ( getWidth() != width )
        {
          setMinWidth( width );
          setMaxWidth( width );
        }
      }
    } );

    // use TextFormatter to set desired caret position
    setTextFormatter( new TextFormatter<>( change ->
    {
      // position caret
      if ( m_caretPos >= 0 )
      {
        change.selectRange( m_caretPos, m_caretPos );
        m_caretPos = -1;
      }
      return change;
    } ) );

  }

  /***************************************** replaceText *****************************************/
  @Override
  public void replaceText( int start, int end, String text )
  {
    // only progress text updates that result in allowed new text
    String oldText = getText();
    String newText = oldText.substring( 0, start ) + text + oldText.substring( end );

    if ( isAllowed( newText ) )
      super.replaceText( start, end, text );
  }

  /***************************************** setCaretPos *****************************************/
  public void setCaretPos( int pos )
  {
    // set caret position to be selected after next setting text
    m_caretPos = pos;
  }

  /****************************************** setAllowed *****************************************/
  public void setAllowed( String regex )
  {
    // regular expression that limits what can be entered into editor
    m_allowed = regex == null ? null : Pattern.compile( regex );
  }

  /****************************************** isAllowed ******************************************/
  public boolean isAllowed( String text )
  {
    // return true if text is allowed
    return m_allowed == null || m_allowed.matcher( text ).matches();
  }

  /****************************************** setWidths ******************************************/
  public void setWidths( double min, double max )
  {
    // set editor minimum and maximum width (only used for table cell editors)
    m_minWidth = min;
    m_maxWidth = max;

    // if button, trigger editor padding calculation by redrawing button
    if ( m_buttonType != null )
      drawButton();
  }

  /****************************************** getButton ******************************************/
  public Canvas getButton()
  {
    // return button canvas
    return m_button;
  }

  /**************************************** setButtonType ****************************************/
  public void setButtonType( ButtonType type )
  {
    // set button type for this editor
    m_buttonType = type;
    m_button = new Canvas();
    m_button.setManaged( false );
    m_button.setCursor( Cursor.DEFAULT );
    getChildren().add( m_button );

    // add listeners to (re)draw button every time editor changes size
    heightProperty().addListener( ( property, oldHeight, newHeight ) -> drawButton() );
    widthProperty().addListener( ( property, oldWidth, newWidth ) -> drawButton() );
  }

  /***************************************** drawButton ******************************************/
  private void drawButton()
  {
    // determine size and draw button
    double h = getHeight() - 2 * BUTTONS_PADDING;
    double w = getWidth() / 2;
    if ( w > BUTTONS_WIDTH_MAX )
      w = BUTTONS_WIDTH_MAX;

    // set editor insets and button position
    double pad = getPadding().getLeft();
    setPadding( new Insets( getPadding().getTop(), w + pad, getPadding().getBottom(), pad ) );
    m_button.setLayoutX( getWidth() - w - BUTTONS_PADDING );
    m_button.setLayoutY( BUTTONS_PADDING );

    // if size has not changed, no need to re-draw
    if ( m_button.getHeight() == h && m_button.getWidth() == w )
      return;

    // set size and fill background
    m_button.setHeight( h );
    m_button.setWidth( w );
    GraphicsContext gc = m_button.getGraphicsContext2D();
    gc.setFill( Colors.BUTTON_BACKGROUND );
    gc.fillRect( 0.0, 0.0, w, h );

    // draw correct button depending on button type
    int x1, y1, y2;
    gc.setStroke( Colors.BUTTON_ARROW );
    switch ( m_buttonType )
    {
      case DOWN:
        // draw down arrow
        x1 = (int) ( w * 0.3 + 0.5 );
        y1 = (int) ( h * 0.3 + 0.5 );
        y2 = (int) ( h - y1 );
        for ( int y = y1; y <= y2; y++ )
        {
          double x = x1 + ( w * 0.5 - x1 ) / ( y2 - y1 ) * ( y - y1 );
          gc.strokeLine( x, y + .5, w - x, y + .5 );
        }
        break;

      case UP_DOWN:
        // draw up+down arrows
        x1 = (int) ( w * 0.2 + 0.5 );
        y1 = (int) ( h * 0.1 + 0.6 );
        y2 = (int) ( h * 0.5 - y1 );
        for ( int y = y1; y <= y2; y++ )
        {
          double x = x1 + ( w * 0.5 - x1 ) / ( y2 - y1 ) * ( y2 - y );
          gc.strokeLine( x, y + .5, w - x, y + .5 );
          gc.strokeLine( x, h - ( y + .5 ), w - x, h - ( y + .5 ) );
        }
        break;

      default:
        throw new IllegalArgumentException( "Type=" + m_buttonType );
    }

  }

}
