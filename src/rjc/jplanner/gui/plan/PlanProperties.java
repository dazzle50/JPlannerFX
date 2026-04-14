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

package rjc.jplanner.gui.plan;

import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
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
import rjc.jplanner.gui.ChooseCalendar;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.control.DateTimeField;
import rjc.table.control.ExpandingField;
import rjc.table.control.IObservableStatus;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.view.Colours;
import rjc.table.view.TableScrollBar;

/*************************************************************************************************/
/******************* Controls for displaying & editing various plan properties *******************/
/*************************************************************************************************/

public class PlanProperties extends ScrollPane
{
  private Plan              m_plan;
  private ObservableStatus  m_status;
  private NumberOf          m_numberOf;

  private GridPane          m_grid         = new GridPane();
  private ExpandingField    m_title        = new ExpandingField();
  private DateTimeField     m_defaultStart = new DateTimeField();
  private ExpandingField    m_actualStart  = new ExpandingField();
  private ExpandingField    m_end          = new ExpandingField();
  private ChooseCalendar    m_defaultCalendar;
  private ExpandingField    m_DTformat     = new ExpandingField();
  private ExpandingField    m_Dformat      = new ExpandingField();
  private ExpandingField    m_fileName     = new ExpandingField();
  private ExpandingField    m_fileLocation = new ExpandingField();
  private ExpandingField    m_savedBy      = new ExpandingField();
  private ExpandingField    m_savedWhen    = new ExpandingField();

  private static Background READONLY       = new Background(
      new BackgroundFill( Colours.CELL_DISABLED_BACKGROUND, null, null ) );

  public static String      STYLE_TOOLTIP;

  /**************************************** constructor ******************************************/
  public PlanProperties( PlanContext context )
  {
    // remember plan and setup gui
    m_plan = context.getPlan();
    m_status = context.getStatus();
    m_numberOf = new NumberOf( m_plan );
    m_defaultCalendar = new ChooseCalendar( m_plan.getCalendars() );
    setup();

    // set style for tool tips
    STYLE_TOOLTIP = "-fx-text-fill: black;";
    STYLE_TOOLTIP += "-fx-background-color: lightyellow;";
    STYLE_TOOLTIP += "-fx-padding: 0.2em 1em 0.2em 0.5em;";
    STYLE_TOOLTIP += "-fx-background-radius: 3px;";
  }

  /******************************************* setup *********************************************/
  private void setup()
  {
    // setup scrolling properties panel
    setMinWidth( 0.0 );

    // set scroll-bar width (later when it has been created)
    Platform.runLater( () ->
    {
      if ( lookup( ".scroll-bar:vertical" ) instanceof ScrollBar scrollbar )
        scrollbar.setPrefWidth( TableScrollBar.SIZE );
    } );

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
    m_grid.add( m_numberOf, 0, row, 2, 1 );
    GridPane.setHgrow( m_numberOf, Priority.ALWAYS );
    GridPane.setVgrow( m_numberOf, Priority.ALWAYS );

    // set tool tips
    Tooltip DTtip = new Tooltip(
        "Symbol\tMeaning\t\t\t\t\tPresentation\tExamples\n" + "--------\t---------\t\t\t\t\t--------------\t---------\n"
            + " G\t\t era\t\t\t\t\t\t text\t\t\t AD; Anno Domini; A\n" + " u\t\t year\t\t\t\t\t\t year\t\t\t 2004; 04\n"
            + " y\t\t year-of-era\t\t\t\t year\t\t\t 2004; 04\n" + " D\t\t day-of-year\t\t\t\t number\t\t 189\n"
            + " M/L\t\t month-of-year\t\t\t number/text\t 7; 07; Jul; July; J\n"
            + " d\t\t day-of-month\t\t\t\t number\t\t 10\n\n"
            + " U\t\t half-of-year\t\t\t\t number/text\t 2; H2; 2nd half\n"
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
    DTtip.setStyle( STYLE_TOOLTIP );
    DTtip.setShowDuration( Duration.INDEFINITE );
    m_DTformat.setTooltip( DTtip );

    Tooltip Dtip = new Tooltip(
        "Symbol\tMeaning\t\t\t\t\tPresentation\tExamples\n" + "--------\t---------\t\t\t\t\t--------------\t---------\n"
            + " G\t\t era\t\t\t\t\t\t text\t\t\t AD; Anno Domini; A\n" + " u\t\t year\t\t\t\t\t\t year\t\t\t 2004; 04\n"
            + " y\t\t year-of-era\t\t\t\t year\t\t\t 2004; 04\n" + " D\t\t day-of-year\t\t\t\t number\t\t 189\n"
            + " M/L\t\t month-of-year\t\t\t number/text\t 7; 07; Jul; July; J\n"
            + " d\t\t day-of-month\t\t\t\t number\t\t 10\n\n"
            + " U\t\t half-of-year\t\t\t\t number/text\t 2; H2; 2nd half\n"
            + " Q/q\t\t quarter-of-year\t\t\t number/text\t 3; 03; Q3; 3rd quarter\n"
            + " Y\t\t week-based-year\t\t\t year\t\t\t 1996; 96\n" + " w\t\t week-of-week-based-year\t number\t\t 27\n"
            + " W\t\t week-of-month\t\t\t number\t\t 4\n" + " E\t\t day-of-week\t\t\t\t text\t\t\t Tue; Tuesday; T\n"
            + " e/c\t\t localized day-of-week\t\t number/text\t 2; 02; Tue; Tuesday; T\n"
            + " F\t\t week-of-month\t\t\t number\t\t 3\n\n" + " p\t\t pad next\t\t\t\t\t pad modifier\t 1\n"
            + " '\t\t escape for text\t\t\t delimiter\n" + " ''\t\t single quote\t\t\t\t literal\t\t '\n" );
    Dtip.setStyle( STYLE_TOOLTIP );
    Dtip.setShowDuration( Duration.INDEFINITE );
    m_Dformat.setTooltip( Dtip );

    // show updated examples of formats
    m_DTformat.textProperty().addListener( ( property, oldText, newText ) -> dateTimeFormatChange() );
    m_DTformat.focusedProperty().addListener( ( property, oldFocus, newFocus ) -> dateTimeFocusChange() );
    m_DTformat.setOnKeyPressed( event -> keyPressed( event ) );
    m_Dformat.textProperty().addListener( ( property, oldText, newText ) -> dateFormatChange() );
    m_Dformat.focusedProperty().addListener( ( property, oldFocus, newFocus ) -> dateFocusChange() );
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
      field.setStatus( m_status );
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
        source.setText( m_plan.getDateTimeFormat() );
      if ( source == m_Dformat )
        source.setText( m_plan.getDateFormat() );
      if ( source == m_title )
        source.setText( m_plan.getTitle() );
      if ( source == m_defaultStart )
        displayDateTime( m_defaultStart, m_plan.getDefaultStart() );
      if ( source == m_defaultCalendar )
        source.setText( m_plan.getDefaultCalendar().getName() );
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

      m_DTformat.getStatus().update( Level.INFO, "Date-time format example: "
          + DateTime.now().toString( DateTimeFormatter.ofPattern( m_DTformat.getText() ) ) );
      m_DTformat.setStyle( m_DTformat.getStatus().getStyle() );
    }
    catch ( Exception exception )
    {
      String err = exception.getMessage();
      m_DTformat.getStatus().update( Level.ERROR, "Date-time format error '" + err + "'" );
      m_DTformat.setStyle( m_DTformat.getStatus().getStyle() );
    }

    displayDateTime( m_actualStart, m_plan.getEarliestTaskStart() );
    displayDateTime( m_end, m_plan.getLatestTaskEnd() );
    displayDateTime( m_savedWhen, m_plan.getSavedWhen() );
  }

  /************************************* dateTimeFocusChange *************************************/
  private void dateTimeFocusChange()
  {
    // react to focus change
    if ( m_DTformat.isFocused() )
      dateTimeFormatChange();
    else
      m_DTformat.getStatus().clear();
  }

  /************************************** dateFormatChange ***************************************/
  private void dateFormatChange()
  {
    // update display for date format change
    try
    {
      if ( m_Dformat.getText().length() < 1 )
        throw new NumberFormatException( "Invalid format" );

      m_Dformat.getStatus().update( Level.INFO, "Date format example: " + Date.now().toString( m_Dformat.getText() ) );
      m_Dformat.setStyle( m_Dformat.getStatus().getStyle() );
    }
    catch ( Exception exception )
    {
      String err = exception.getMessage();
      m_Dformat.getStatus().update( Level.ERROR, "Date format error '" + err + "'" );
      m_Dformat.setStyle( m_Dformat.getStatus().getStyle() );
    }
  }

  /*************************************** dateFocusChange ***************************************/
  private void dateFocusChange()
  {
    // react to focus change
    if ( m_Dformat.isFocused() )
      dateFormatChange();
    else
      m_Dformat.getStatus().clear();
  }

  /*************************************** displayDateTime ***************************************/
  private void displayDateTime( TextField field, DateTime dt )
  {
    // display date-time in user specified format, or if that fails in plan format
    if ( dt == null )
      field.setText( null );
    else if ( m_DTformat.getStatus().isError() )
      field.setText( dt.toString( DateTimeFormatter.ofPattern( m_plan.getDateTimeFormat() ) ) );
    else
      field.setText( dt.toString( DateTimeFormatter.ofPattern( m_DTformat.getText() ) ) );
  }

  /**************************************** updateFromPlan ***************************************/
  public void updateFromPlan()
  {
    // update the gui property widgets with values from plan
    m_title.setText( m_plan.getTitle() );
    m_defaultCalendar.setSelected( m_plan.getDefaultCalendar().getName() );
    m_DTformat.setText( m_plan.getDateTimeFormat() );
    m_Dformat.setText( m_plan.getDateFormat() );
    m_fileName.setText( m_plan.getFilename() );
    m_fileLocation.setText( m_plan.getFileLocation() );
    m_savedBy.setText( m_plan.getSavedBy() );

    m_defaultStart.setDateTime( m_plan.getDefaultStart() );
    displayDateTime( m_actualStart, m_plan.getEarliestTaskStart() );
    displayDateTime( m_end, m_plan.getLatestTaskEnd() );
    displayDateTime( m_savedWhen, m_plan.getSavedWhen() );

    // update the gui "number of" pane
    m_numberOf.redraw();
  }

  /***************************************** updatePlan ******************************************/
  public void updatePlan()
  {
    // TODO get values from gui editors
  }

}
