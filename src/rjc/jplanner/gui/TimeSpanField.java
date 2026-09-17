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

package rjc.jplanner.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.TimeSpan.Unit;
import rjc.table.control.ButtonField;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/***************************** Spin control for entering time-spans ******************************/
/*************************************************************************************************/

public class TimeSpanField extends ButtonField
{
  private final DecimalFormat m_format;               // numeric display format
  private TimeSpan            m_span;                 // last valid time-span, null when blank
  private Unit                m_unit  = Unit.SECONDS; // selected unit, retained while blank
  private boolean             m_updating;             // suppress parsing while normalising text
  private boolean             m_valid = true;         // current text is valid, including blank input

  /**************************************** constructor ******************************************/
  public TimeSpanField()
  {
    // retain the standard spin buttons, range and numeric digit limit
    setRange( 0, 9999 );
    setButtonType( ButtonType.UP_DOWN );

    m_format = new DecimalFormat( "0", DecimalFormatSymbols.getInstance( Locale.ROOT ) );
    m_format.setMaximumIntegerDigits( 6 );

    textProperty().addListener( ( property, oldText, newText ) -> checkText() );
    focusedProperty().addListener( ( property, oldFocus, newFocus ) -> checkFocus( newFocus ) );
    setValue( new TimeSpan( "0S" ) );

    // ensure caret and anchor positions stay in the number, including when no suffix is shown
    caretPositionProperty().addListener( ( property, oldPos, newPos ) ->
    {
      int maxPos = numberEnd();
      if ( newPos.intValue() > maxPos )
        positionCaret( maxPos );
    } );

    anchorProperty().addListener( ( property, oldPos, newPos ) ->
    {
      int maxPos = numberEnd();
      if ( newPos.intValue() > maxPos )
        selectRange( maxPos, getCaretPosition() );
    } );

    // a unit shortcut changes units without inserting a letter into the number
    addEventFilter( KeyEvent.KEY_TYPED, event ->
    {
      String text = event.getCharacter();
      if ( text.length() == 1 && Unit.fromChar( text.charAt( 0 ) ) != null )
      {
        setValue( text );
        event.consume();
      }
    } );

    // restore the last valid value on Escape; the table editor handles cancellation of the edit
    addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( event.getCode() == KeyCode.ESCAPE )
        setValue( m_span );
    } );
  }

  /***************************************** numberEnd *******************************************/
  private int numberEnd()
  {
    // getValue also handles malformed text that does not contain the current suffix
    return getValue().length();
  }

  /***************************************** checkFocus ******************************************/
  private void checkFocus( boolean newFocus )
  {
    // retain errors until the owning editor has decided whether the edit can be committed
    if ( !m_valid || getStatus() != null && getStatus().isError() )
      return;
    if ( newFocus )
      showStatus();
    else
    {
      setValue( m_span );
      if ( getStatus() != null && !getStatus().isError() )
        getStatus().clear();
    }
  }

  /****************************************** checkText ******************************************/
  private void checkText()
  {
    if ( m_updating )
      return;

    // separate the unit from the editable number, retaining unfinished decimals such as 1.
    String number = getText() == null ? "" : getText().replaceAll( "\\s+", "" );
    Unit unit = number.isEmpty() ? null : Unit.fromChar( number.charAt( number.length() - 1 ) );

    if ( unit != null )
      number = number.substring( 0, number.length() - 1 );
    if ( number.equalsIgnoreCase( "None" ) )
      number = "";
    m_valid = number.matches( "[0-9]*\\.?[0-9]*" );

    if ( m_valid )
    {
      m_unit = unit == null ? m_unit : unit;
      number = number.replaceFirst( "^0+(?=[0-9])", "" );
      m_span = number.isEmpty() ? null : new TimeSpan( number + m_unit.abbreviation() );

      // save the value before notifying owners; changing ButtonField's suffix does not rewrite text
      setPrefixSuffix( null, m_span == null ? "" : " " + m_unit.abbreviation() );
      int caret = getCaretPosition();
      int anchor = getAnchor();
      m_updating = true;
      try
      {
        // setting even identical text resets JavaFX's text undo history
        String text = number + getSuffix();
        if ( !text.equals( getText() ) )
          setText( text );
        selectRange( Math.min( anchor, number.length() ), Math.min( caret, number.length() ) );
      }
      finally
      {
        m_updating = false;
      }
    }

    // report after formatting, before the owner's text listener applies its validation rules
    showStatus();
  }

  /***************************************** showStatus ******************************************/
  private void showStatus()
  {
    // malformed text retains the last valid time-span for recovery, but must report an error
    if ( getStatus() != null )
    {
      if ( m_valid )
        getStatus().update( Level.INFO, m_span == null ? "" : m_span.toStringLong() );
      else
        getStatus().update( Level.ERROR, "Invalid duration" );
    }
  }

  /***************************************** isAllowed *******************************************/
  @Override
  public boolean isAllowed( String text )
  {
    // use the remembered unit even when changing units on an already blank field
    return text.matches( "[0-9]{0,6}" + ( m_unit == Unit.SECONDS ? "" : "(\\.[0-9]{0,2})?" ) + getSuffix() );
  }

  /***************************************** replaceText *****************************************/
  @Override
  public void replaceText( int start, int end, String text )
  {
    String candidate = getText().substring( 0, start ) + text + getText().substring( end );
    String number = candidate.endsWith( getSuffix() )
        ? candidate.substring( 0, candidate.length() - getSuffix().length() )
        : candidate;
    String compact = text.replaceAll( "\\s+", "" );

    // allow None to be typed progressively, keeping incomplete text invalid until it is finished
    if ( !number.isEmpty() && number.length() <= 4 && "None".regionMatches( true, 0, number, 0, number.length() ) )
      candidate = number;
    // a complete pasted time-span replaces the existing suffix and retains paste rounding
    else if ( start == 0 && end >= numberEnd()
        && ( compact.equalsIgnoreCase( "None" ) || compact.matches( "[0-9.].*[SMHdwmy]" ) ) )
      candidate = compact;
    else if ( candidate.isBlank() || candidate.equals( getSuffix() ) )
      candidate = "";
    else
    {
      boolean firstNumber = getText().isEmpty() && isAllowed( candidate );
      boolean recovery = !m_valid && candidate.replaceAll( "\\s+", "" ).matches( "[0-9]*\\.?[0-9]*[SMHdwmy]?" );
      if ( !firstNumber && !recovery )
      {
        // ordinary numeric edits retain the inherited input filter and text undo behaviour
        super.replaceText( start, end, text );
        return;
      }
    }

    // apply suffix changes outside JavaFX's replacement operation to keep its edit indexes valid
    setText( candidate );
    positionCaret( Math.min( start + text.length(), numberEnd() ) );
  }

  /****************************************** setValue *******************************************/
  @Override
  public void setValue( Object value )
  {
    if ( value instanceof String text )
    {
      String compact = text.replaceAll( "\\s+", "" );
      if ( compact.length() == 1 && Unit.fromChar( compact.charAt( 0 ) ) != null )
      {
        // a unit shortcut retains blank, and rounds an existing value using TimeSpan's rules
        m_unit = Unit.fromChar( compact.charAt( 0 ) );
        value = m_span == null ? null : new TimeSpan( m_span.getNumber(), m_unit );
      }
      else
      {
        setText( compact );
        return;
      }
    }

    if ( value instanceof TimeSpan span )
    {
      m_unit = span.getUnit();
      value = span.getNumber();
    }

    // format assigned numbers; text input keeps unfinished decimals until focus leaves
    m_format.setMaximumFractionDigits( m_unit == Unit.SECONDS ? 0 : 2 );
    setText( value == null ? "" : m_format.format( value ) + " " + m_unit.abbreviation() );
    positionCaret( numberEnd() );
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta )
  {
    // the first spin in either direction starts a blank value at one
    if ( m_valid && m_span == null )
      setValue( new TimeSpan( 1, m_unit ) );
    else
      super.changeValue( delta );
  }

  /**************************************** isValueValid *****************************************/
  public boolean isValueValid()
  {
    // blank is valid; malformed text must not commit the saved recovery value
    return m_valid;
  }

  /***************************************** getTimeSpan *****************************************/
  public TimeSpan getTimeSpan()
  {
    // copy mutable values so callers and undo commands cannot change the editor's saved value
    return TimeSpan.copyOf( m_span );
  }
}
