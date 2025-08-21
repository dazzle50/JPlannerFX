/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.TimeSpan.UNITS;
import rjc.table.control.NumberSpinField;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/***************************** Spin control for entering time-spans ******************************/
/*************************************************************************************************/

public class TimeSpanField extends NumberSpinField
{
  private TimeSpan m_span;

  /**************************************** constructor ******************************************/
  public TimeSpanField()
  {
    // set default editor characteristics
    setRange( 0, 9999 );
    setValue( new TimeSpan( "0S" ) );

    // add listener for text and focus changes
    textProperty().addListener( ( property, oldText, newText ) -> checkText( newText ) );
    focusedProperty().addListener( ( property, oldFocus, newFocus ) -> checkFocus( newFocus ) );

    // ensure caret position stays in number
    caretPositionProperty().addListener( ( property, oldPos, newPos ) ->
    {
      int maxPos = getText().length() - 2;
      if ( newPos.intValue() > maxPos )
        positionCaret( maxPos );
    } );

    // ensure anchor position stays in number
    anchorProperty().addListener( ( property, oldPos, newPos ) ->
    {
      int maxPos = getText().length() - 2;
      if ( newPos.intValue() > maxPos )
        selectRange( maxPos, this.getCaretPosition() );
    } );

    // update units if key typed matches unit first character
    setOnKeyTyped( event ->
    {
      char ch = event.getCharacter().charAt( 0 );
      var unit = UNITS.fromChar( ch );
      if ( unit != null )
        setValue( new TimeSpan( m_span.getNumber(), unit ) );
    } );

    // if ESC pressed ensure text reflects last valid time-span
    addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( event.getCode() == KeyCode.ESCAPE )
        setText( m_span.toString() );
    } );
  }

  /***************************************** checkFocus ******************************************/
  private void checkFocus( Boolean newFocus )
  {
    // update status and ensure valid time-span is shown when focus lost
    if ( newFocus )
      checkText( getText() );
    else
    {
      setText( m_span.toString() );
      getStatus().clear();
    }
  }

  /****************************************** checkText ******************************************/
  private void checkText( String text )
  {
    // ensure only integers for seconds, otherwise 2 decimals places are allowed
    var lastChar = text.charAt( text.length() - 1 );
    if ( lastChar == UNITS.SECONDS.abbreviation() )
      setFormat( "0", 8, 0 );
    else
      setFormat( "0", 8, 2 );

    // if valid time-span, store it and update status
    if ( Character.isDigit( text.charAt( 0 ) ) || text.charAt( 0 ) == '.' )
    {
      m_span = new TimeSpan( text );
      if ( getStatus() != null )
      {
        getStatus().update( Level.NORMAL, m_span.toStringLong() );
      }
    }
    else
    {
      if ( getStatus() != null )
        getStatus().update( Level.ERROR, "Invalid duration" );
    }
  }

  /****************************************** setValue *******************************************/
  @Override
  public void setValue( Object value )
  {
    // if time-span then set both suffix and number
    if ( value instanceof TimeSpan ts )
    {
      m_span = ts;
      setPrefixSuffix( null, " " + ts.getUnits().abbreviation() );
      super.setValue( ts.getNumber() );
    }
    else if ( value instanceof String str )
    {
      char ch = str.charAt( 0 );
      if ( !Character.isDigit( ch ) && ch != '.' )
      {
        var units = UNITS.fromChar( ch );
        if ( units != null )
        {
          m_span.setUnits( units );
          setValue( m_span );
        }
        return;
      }
      super.setValue( str );
    }
    else
      super.setValue( value );
  }

  /***************************************** getTimeSpan *****************************************/
  public TimeSpan getTimeSpan()
  {
    // return current time-span value
    return m_span;
  }

}
