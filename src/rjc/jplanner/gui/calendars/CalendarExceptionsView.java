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

package rjc.jplanner.gui.calendars;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.days.DayTypes;
import rjc.table.control.ChooseField;
import rjc.table.control.DateField;
import rjc.table.data.types.Date;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.view.TableContextMenu;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.cursor.Cursors;
import rjc.table.view.editor.AbstractCellEditor;

/*************************************************************************************************/
/************************** Editable view of draft calendar exceptions ***************************/
/*************************************************************************************************/

public class CalendarExceptionsView extends TableView
{
  private final CalendarExceptionsData m_data;     // draft rows and validation
  private final Calendar               m_calendar; // normal cycle used for new exceptions
  private final DayTypes               m_days;     // available plan-owned day types
  private ExceptionEditor              m_editor;   // currently open editor, never an editability probe

  /**************************************** constructor ******************************************/
  public CalendarExceptionsView( CalendarExceptionsData data, Calendar calendar, DayTypes days )
  {
    super( data, "Calendar exceptions" );
    m_data = data;
    m_calendar = calendar;
    m_days = days;
    TableContextMenu.disableAll( this );
    setFocusTraversable( true );

    // preserve multi-row selection instead of the standard table's right-click clearing it
    addEventFilter( MouseEvent.MOUSE_PRESSED, event ->
    {
      if ( event.getButton() == MouseButton.SECONDARY )
      {
        if ( m_editor == null )
          requestFocus();
        event.consume();
      }
    } );

    // keep chronological rows and fixed column order while retaining header selection and resizing
    cursorProperty().addListener( ( property, oldCursor, newCursor ) ->
    {
      if ( newCursor == Cursors.ROWS_REORDER )
        setCursor( Cursors.ROWS_HOVER );
      if ( newCursor == Cursors.COLUMNS_REORDER )
        setCursor( Cursors.COLUMNS_HOVER );
    } );
    addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( m_editor != null )
        return;
      if ( event.getCode() == KeyCode.DELETE )
        removeSelected();
      else if ( event.getCode() == KeyCode.INSERT )
        addException();
      else
        return;
      event.consume();
    } );
  }

  /****************************************** relocate *******************************************/
  @Override
  public void relocate( double x, double y )
  {
    // do nothing when relocate called - to prevent jitter when using split-pane
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    super.reset();
    getColumnsAxis().setDefaultNominalSize( 160 );
  }

  /**************************************** getCellDrawer ****************************************/
  @Override
  public CellDrawer getCellDrawer()
  {
    // format only the display; keep Date values available to validation and the cell editor
    return new CellDrawer()
    {
      /****************************************** getText ****************************************/
      @Override
      protected String getText()
      {
        return value instanceof Date date ? date.format( "dd MMM yyyy - EEEE" ) : super.getText();
      }
    };
  }

  /**************************************** getCellEditor ****************************************/
  @Override
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    if ( cell.dataColumn < 0 || cell.dataColumn > 1 || m_data.getDate( cell.dataRow ) == null )
      return null;
    return new ExceptionEditor( cell.dataColumn, cell.dataRow );
  }

  /***************************************** openEditor ******************************************/
  @Override
  public void openEditor( Object value )
  {
    // capture the intended date before committing another editor can change row order
    int column = getColumnsAxis().getDataIndex( getFocusCell().getColumn() );
    Date date = m_data.getDate( getRowsAxis().getDataIndex( getFocusCell().getRow() ) );
    if ( !finishEditing() )
      return;
    if ( date != null && m_data.indexOf( date ) >= 0 )
      selectDate( date, column );
    super.openEditor( value );
  }

  /****************************************** isEditing ******************************************/
  public boolean isEditing()
  {
    return m_editor != null;
  }

  /**************************************** finishEditing ****************************************/
  public boolean finishEditing()
  {
    return m_editor == null || m_editor.finish();
  }

  /**************************************** cancelEditing ****************************************/
  public void cancelEditing()
  {
    if ( m_editor != null )
      m_editor.discard();
  }

  /***************************************** selectDate ******************************************/
  public void selectDate( Date date, int column )
  {
    int row = m_data.indexOf( date );
    getSelection().clear();
    getFocusCell().setPosition( column, row );
    getSelectCell().setPosition( column, row );
    if ( row >= 0 )
    {
      getSelection().select( 0, row, 1, row );
      scrollTo( getFocusCell() );
    }
  }

  /**************************************** addException *****************************************/
  private void addException()
  {
    if ( !finishEditing() )
      return;
    Date date = m_data.addException( Date.now(), m_calendar );
    if ( date == null )
    {
      getStatus().update( Level.ERROR, "No unused date is available after today" );
      return;
    }
    selectDate( date, 0 );
    openEditor( date );
  }

  /**************************************** selectedDates ****************************************/
  private List<Date> selectedDates()
  {
    var dates = new ArrayList<Date>();
    for ( int row = 0; row < m_data.getRowCount(); row++ )
      if ( getSelection().hasRowSelection( row ) )
        dates.add( m_data.getDate( getRowsAxis().getDataIndex( row ) ) );
    return dates;
  }

  /**************************************** removeSelected ***************************************/
  private void removeSelected()
  {
    if ( !finishEditing() )
      return;
    m_data.removeExceptions( selectedDates() );
    selectDate( m_data.getDate( 0 ), 0 );
  }

  /*************************************** openContextMenu ***************************************/
  @Override
  public void openContextMenu( ContextMenuEvent event )
  {
    if ( !finishEditing() )
      return;

    // right-click on an unselected row targets it; existing multi-row selections remain intact
    int row = getMouseCell().getRow();
    if ( !event.isKeyboardTrigger() && row >= 0 && row < m_data.getRowCount()
        && !getSelection().hasRowSelection( row ) )
      selectDate( m_data.getDate( getRowsAxis().getDataIndex( row ) ), 0 );

    var add = new MenuItem( "Add exception" );
    add.setOnAction( action -> addException() );
    var remove = new MenuItem( "Remove selected exceptions" );
    remove.setDisable( selectedDates().isEmpty() );
    remove.setOnAction( action -> removeSelected() );
    new ContextMenu( add, remove ).show( getScene().getWindow(), event.getScreenX(), event.getScreenY() );
  }

  // draft editors retain invalid input on focus loss; only explicit cancellation abandons it
  private final class ExceptionEditor extends AbstractCellEditor
  {
    private final int         m_column; // column whose value is being edited
    private final int         m_row;    // stable row until this editor has finished
    private final DateField   m_date;   // date field, or null when editing a day type
    private final ChooseField m_day;    // day-type field, or null when editing a date

    /**************************************** constructor ****************************************/
    private ExceptionEditor( int column, int row )
    {
      m_column = column;
      m_row = row;
      if ( column == 0 )
      {
        m_day = null;
        m_date = new DateField()
        {
          /************************************** validText **************************************/
          @Override
          protected void validText()
          {
            // do not replace invalid text with the previous date when OK receives focus
          }
        };
        setControl( m_date );
      }
      else
      {
        m_date = null;
        m_day = new ChooseField( m_days.asList() );
        setControl( m_day );
      }

      getControl().addEventFilter( KeyEvent.KEY_PRESSED, event ->
      {
        if ( event.getCode() == KeyCode.ESCAPE )
          discard();
        else if ( event.getCode() == KeyCode.ENTER )
          finish();
        else if ( event.getCode() == KeyCode.TAB )
        {
          if ( finish() )
          {
            int columnToSelect = event.isShiftDown() ? 0 : 1;
            selectDate( m_data.getDate( getFocusCell().getRow() ), columnToSelect );
          }
        }
        else
          return;
        event.consume();
      } );
    }

    /******************************************** open *******************************************/
    @Override
    public void open( Object value, CellDrawer cell )
    {
      m_editor = this;
      super.open( value, cell );
    }

    /****************************************** getValue *****************************************/
    @Override
    public Object getValue()
    {
      if ( m_day != null )
        return m_day.getSelected();
      try
      {
        // validation must inspect the entered text, not DateField's cached last valid date
        return Date.parse( m_date.getText().strip() );
      }
      catch ( DateTimeException | IllegalArgumentException | ArithmeticException exception )
      {
        return null;
      }
    }

    /****************************************** setValue *****************************************/
    @Override
    public void setValue( Object value )
    {
      if ( m_date != null )
        m_date.setDate( value );
      else
      {
        try
        {
          m_day.setSelected( value );
        }
        catch ( java.security.InvalidParameterException exception )
        {
          // unmatched starter text leaves the existing day-type choice selected
          if ( !( value instanceof String ) )
            throw exception;
        }
      }
    }

    /******************************************* close *******************************************/
    @Override
    public void close( boolean commit )
    {
      // the base editor requests close(false) on invalid focus loss; keep that edit visible
      finish();
    }

    /****************************************** discard ******************************************/
    private void discard()
    {
      m_editor = null;
      super.close( false );
    }

    /******************************************* finish ******************************************/
    private boolean finish()
    {
      // validate and save the draft value, keeping the editor open if the value is rejected
      String error = m_data.setValue( m_column, m_row, getValue() );
      if ( error != null )
      {
        getStatus().update( Level.ERROR, error );
        getControl().setStyle( getStatus().getStyle() );
        return false;
      }

      // close before changing row order; these draft edits never enter the plan undo stack
      discard();
      Date date = m_data.getDate( m_row );
      m_data.refresh();
      selectDate( date, m_column );
      return true;
    }
  }
}
