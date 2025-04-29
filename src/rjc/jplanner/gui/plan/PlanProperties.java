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

package rjc.jplanner.gui.plan;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import rjc.jplanner.Main;
import rjc.jplanner.gui.CalendarChoose;
import rjc.jplanner.gui.MainWindow;
import rjc.table.control.DateTimeField;
import rjc.table.control.ExpandingField;
import rjc.table.control.IObservableStatus;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.view.Colours;

/*************************************************************************************************/
/******************* Controls for displaying & editing various plan properties *******************/
/*************************************************************************************************/

public class PlanProperties extends ScrollPane
{
  private GridPane          m_grid            = new GridPane();
  private ExpandingField    m_title           = new ExpandingField();
  private DateTimeField     m_defaultStart    = new DateTimeField();
  private ExpandingField    m_actualStart     = new ExpandingField();
  private ExpandingField    m_end             = new ExpandingField();
  private CalendarChoose    m_defaultCalendar = new CalendarChoose();
  private ExpandingField    m_DTformat        = new ExpandingField();
  private ExpandingField    m_Dformat         = new ExpandingField();
  private ExpandingField    m_fileName        = new ExpandingField();
  private ExpandingField    m_fileLocation    = new ExpandingField();
  private ExpandingField    m_savedBy         = new ExpandingField();
  private ExpandingField    m_savedWhen       = new ExpandingField();
  // private NumberOf m_numberOf = new NumberOf();

  private static Background READONLY          = new Background(
      new BackgroundFill( Colours.CELL_DISABLED_BACKGROUND, null, null ) );

  /**************************************** constructor ******************************************/
  public PlanProperties()
  {
    // setup scrolling properties panel
    setMinWidth( 0.0 );

    // grid for properties layout
    m_grid.setVgap( 5.0 );
    m_grid.setHgap( 5.0 );
    m_grid.setPadding( new Insets( 5.0 ) );
    setContent( m_grid );
    setFitToWidth( true );
    setFitToHeight( true );

    // add field rows
    int row = 0;
    addRow( row++, "Title", m_title, false );
    addRow( row++, "Default Start", m_defaultStart, false );
    addRow( row++, "Actual Start", m_actualStart, true );
    addRow( row++, "End", m_end, true );
    addRow( row++, "Default Calendar", m_defaultCalendar, false );
    addRow( row++, "Date-time format", m_DTformat, false );
    addRow( row++, "Date format", m_Dformat, false );
    addRow( row++, "File name", m_fileName, true );
    addRow( row++, "File location", m_fileLocation, true );
    addRow( row++, "Saved by", m_savedBy, true );
    addRow( row++, "Saved when", m_savedWhen, true );

    // add number of
    row++;
    // m_grid.add( m_numberOf, 0, row, 2, 1 );
    // GridPane.setHgrow( m_numberOf, Priority.ALWAYS );
    // GridPane.setVgrow( m_numberOf, Priority.ALWAYS );

    // set tool tips
    Tooltip DTtip = new Tooltip(
        "Symbol\tMeaning\t\t\t\t\tPresentation\tExamples\n" + "--------\t---------\t\t\t\t\t--------------\t---------\n"
            + " G\t\t era\t\t\t\t\t\t text\t\t\t AD; Anno Domini; A\n" + " u\t\t year\t\t\t\t\t\t year\t\t\t 2004; 04\n"
            + " y\t\t year-of-era\t\t\t\t year\t\t\t 2004; 04\n" + " D\t\t day-of-year\t\t\t\t number\t\t 189\n"
            + " M/L\t\t month-of-year\t\t\t number/text\t 7; 07; Jul; July; J\n"
            + " d\t\t day-of-month\t\t\t\t number\t\t 10\n\n"
            + " B\t\t half-of-year\t\t\t\t number/text\t 2; H2; 2nd half\n"
            + " Q/q\t\t quarter-of-year\t\t\t number/text\t 3; 03; Q3; 3rd quarter\n"
            + " Y\t\t week-based-year\t\t\t year\t\t\t 1996; 96\n" + " w\t\t week-of-week-based-year\t number\t\t 27\n"
            + " W\t\t week-of-month\t\t\t number\t\t 4\n" + " E\t\t day-of-week\t\t\t\t text\t\t\t Tue; Tuesday; T\n"
            + " e/c\t\t localized day-of-week\t\t number/text\t 2; 02; Tue; Tuesday; T\n"
            + " F\t\t week-of-month\t\t\t number\t\t 3\n\n" + " a\t\t am-pm-of-day\t\t\t text\t\t\t PM\n"
            + " h\t\t clock-hour-of-am-pm (1-12)\t number\t\t 12\n" + " K\t\t hour-of-am-pm (0-11)\t\t number\t\t 0\n"
            + " k\t\t clock-hour-of-am-pm (1-24)\t number\t\t 0\n\n" + " H\t\t hour-of-day (0-23)\t\t\t number\t\t 0\n"
            + " m\t\t minute-of-hour\t\t\t number\t\t 30\n" + " s\t\t second-of-minute\t\t\t number\t\t 55\n"
            + " S\t\t fraction-of-second\t\t\t fraction\t\t 978\n" + " A\t\t milli-of-day\t\t\t\t number\t\t 1234\n"
            + " N\t\t nano-of-day\t\t\t\t number\t\t 1234000000\n\n" + " p\t\t pad next\t\t\t\t\t pad modifier\t 1\n"
            + " '\t\t escape for text\t\t\t delimiter\n" + " ''\t\t single quote\t\t\t\t literal\t\t '\n" );
    DTtip.setStyle( MainWindow.STYLE_TOOLTIP );
    DTtip.setShowDuration( Duration.INDEFINITE );
    m_DTformat.setTooltip( DTtip );

    Tooltip Dtip = new Tooltip(
        "Symbol\tMeaning\t\t\t\t\tPresentation\tExamples\n" + "--------\t---------\t\t\t\t\t--------------\t---------\n"
            + " G\t\t era\t\t\t\t\t\t text\t\t\t AD; Anno Domini; A\n" + " u\t\t year\t\t\t\t\t\t year\t\t\t 2004; 04\n"
            + " y\t\t year-of-era\t\t\t\t year\t\t\t 2004; 04\n" + " D\t\t day-of-year\t\t\t\t number\t\t 189\n"
            + " M/L\t\t month-of-year\t\t\t number/text\t 7; 07; Jul; July; J\n"
            + " d\t\t day-of-month\t\t\t\t number\t\t 10\n\n"
            + " B\t\t half-of-year\t\t\t\t number/text\t 2; H2; 2nd half\n"
            + " Q/q\t\t quarter-of-year\t\t\t number/text\t 3; 03; Q3; 3rd quarter\n"
            + " Y\t\t week-based-year\t\t\t year\t\t\t 1996; 96\n" + " w\t\t week-of-week-based-year\t number\t\t 27\n"
            + " W\t\t week-of-month\t\t\t number\t\t 4\n" + " E\t\t day-of-week\t\t\t\t text\t\t\t Tue; Tuesday; T\n"
            + " e/c\t\t localized day-of-week\t\t number/text\t 2; 02; Tue; Tuesday; T\n"
            + " F\t\t week-of-month\t\t\t number\t\t 3\n\n" + " p\t\t pad next\t\t\t\t\t pad modifier\t 1\n"
            + " '\t\t escape for text\t\t\t delimiter\n" + " ''\t\t single quote\t\t\t\t literal\t\t '\n" );
    Dtip.setStyle( MainWindow.STYLE_TOOLTIP );
    Dtip.setShowDuration( Duration.INDEFINITE );
    m_Dformat.setTooltip( Dtip );

    // show updated examples of formats
    m_DTformat.textProperty().addListener( ( observable, oldValue, newValue ) -> dateTimeFormatChange() );
    m_DTformat.setOnKeyPressed( event -> keyPressed( event ) );
    m_Dformat.textProperty().addListener( ( observable, oldValue, newValue ) -> dateFormatChange() );
    m_Dformat.setOnKeyPressed( event -> keyPressed( event ) );

    // if escape pressed, revert, if return, commit
    m_title.addEventHandler( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );
    m_defaultStart.addEventHandler( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );
    m_defaultCalendar.addEventHandler( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );
  }

  /******************************************** addRow *******************************************/
  private void addRow( int row, String text, Region control, boolean readonly )
  {
    // add row to grid
    Label label = new Label( text );
    m_grid.addRow( row, label, control );
    GridPane.setHalignment( label, HPos.RIGHT );
    GridPane.setHgrow( control, Priority.ALWAYS );

    // if want control to be read-only, set un-editable and background
    if ( readonly && control instanceof TextField field )
    {
      field.setEditable( false );
      field.setBackground( READONLY );
    }

    // attach the control to application status if has observable status
    if ( control instanceof IObservableStatus field )
      field.setStatus( Main.getStatus() );
  }

  /***************************************** keyPressed ******************************************/
  private void keyPressed( KeyEvent event )
  {
    // event source is the text field
    TextField source = (TextField) event.getSource();

    // if escape pressed, revert back to plan format
    if ( event.getCode() == KeyCode.ESCAPE )
    {
      int pos = source.getCaretPosition();
      if ( source == m_DTformat )
        source.setText( Main.getPlan().getDateTimeFormat() );
      if ( source == m_Dformat )
        source.setText( Main.getPlan().getDateFormat() );
      if ( source == m_title )
        source.setText( Main.getPlan().getTitle() );
      if ( source == m_defaultStart )
        displayDateTime( m_defaultStart, Main.getPlan().getDefaultStart() );
      if ( source == m_defaultCalendar )
        source.setText( Main.getPlan().getDefaultCalendar().getName() );
      source.selectRange( pos, pos );
    }

    // if enter pressed, update plan
    if ( event.getCode() == KeyCode.ENTER )
    {
      int pos = source.getCaretPosition();
      updatePlan();
      source.selectRange( pos, pos );
    }
  }

  /************************************* dateTimeFormatChange ************************************/
  private void dateTimeFormatChange()
  {
    // update display for date-time format change
    try
    {
      if ( m_DTformat.getText().length() < 1 )
        throw new NumberFormatException( "Invalid format" );

      m_DTformat.getStatus().update( Level.NORMAL,
          "Date-time format example: " + DateTime.now().toString( m_DTformat.getText() ) );
    }
    catch ( Exception exception )
    {
      String err = exception.getMessage();
      m_DTformat.getStatus().update( Level.ERROR, "Date-time format error '" + err + "'" );
    }

    // displayDateTime( m_actualStart, Main.getPlan().getEarliestTaskStart() );
    // displayDateTime( m_end, Main.getPlan().getLatestTaskEnd() );
    displayDateTime( m_savedWhen, Main.getPlan().getSavedWhen() );
  }

  /*************************************** dateFormatChange **************************************/
  private void dateFormatChange()
  {
    // update display for date format change
    try
    {
      if ( m_Dformat.getText().length() < 1 )
        throw new NumberFormatException( "Invalid format" );

      m_Dformat.getStatus().update( Level.NORMAL,
          "Date format example: " + Date.now().toString( m_Dformat.getText() ) );
    }
    catch ( Exception exception )
    {
      String err = exception.getMessage();
      m_Dformat.getStatus().update( Level.ERROR, "Date format error '" + err + "'" );
    }
  }

  /*************************************** displayDateTime ***************************************/
  private void displayDateTime( TextField field, DateTime dt )
  {
    // display date-time in user specified format, or if that fails in plan format
    if ( dt == null )
      field.setText( null );
    else if ( m_DTformat.getStatus().isError() )
      field.setText( dt.toString( Main.getPlan().getDateTimeFormat() ) );
    else
      field.setText( dt.toString( m_DTformat.getText() ) );
  }

  /**************************************** updateFromPlan ***************************************/
  public void updateFromPlan()
  {
    // update the gui property widgets with values from plan
    m_title.setText( Main.getPlan().getTitle() );
    m_defaultCalendar.setSelected( Main.getPlan().getDefaultCalendar() );
    m_DTformat.setText( Main.getPlan().getDateTimeFormat() );
    m_Dformat.setText( Main.getPlan().getDateFormat() );
    m_fileName.setText( Main.getPlan().getFilename() );
    m_fileLocation.setText( Main.getPlan().getFileLocation() );
    m_savedBy.setText( Main.getPlan().getSavedBy() );

    m_defaultStart.setDateTime( Main.getPlan().getDefaultStart() );
    // displayDateTime( m_actualStart, Main.getPlan().getEarliestTaskStart() );
    // displayDateTime( m_end, Main.getPlan().getLatestTaskEnd() );
    displayDateTime( m_savedWhen, Main.getPlan().getSavedWhen() );

    // update the gui "number of" pane
    // m_numberOf.redraw();
  }

  /***************************************** updatePlan ******************************************/
  public void updatePlan()
  {
    // get values from gui editors
  }

}
