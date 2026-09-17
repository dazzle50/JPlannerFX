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

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
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
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.util.Duration;
import rjc.jplanner.gui.ChooseCalendar;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.TimeSpanField;
import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.calenders.Calendar;
import rjc.table.control.DateTimeField;
import rjc.table.control.ExpandingField;
import rjc.table.control.IObservableStatus;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.IUndoCommand;
import rjc.table.undo.UndoStack;
import rjc.table.view.Colours;
import rjc.table.view.TableScrollBar;

/*************************************************************************************************/
/******************* Controls for displaying & editing various plan properties *******************/
/*************************************************************************************************/

public class PlanProperties extends ScrollPane
{
  private final Plan                 m_plan;
  private final ObservableStatus     m_status;
  private final UndoStack            m_undoStack;
  private final NumberOf             m_numberOf;

  private final GridPane             m_grid                = new GridPane();
  private final ExpandingField       m_title               = new ExpandingField();
  private final DateTimeField        m_defaultStart        = new DateTimeField();
  private final ExpandingField       m_actualStart         = new ExpandingField();
  private final ExpandingField       m_end                 = new ExpandingField();
  private final ChooseCalendar       m_defaultCalendar;
  private final TimeSpanField        m_defaultEndRounding  = new TimeSpanField();                                       // rounding copied into new tasks
  private final ExpandingField       m_dateTimeFormat      = new ExpandingField();
  private final ExpandingField       m_dateFormat          = new ExpandingField();
  private final ExpandingField       m_fileName            = new ExpandingField();
  private final ExpandingField       m_fileLocation        = new ExpandingField();
  private final ExpandingField       m_savedBy             = new ExpandingField();
  private final ExpandingField       m_savedWhen           = new ExpandingField();

  private static final Background    READONLY              = new Background(
      new BackgroundFill( Colours.CELL_DISABLED_BACKGROUND, null, null ) );

  private static final String        TOOLTIP_STYLE         = "-fx-text-fill: black;"
      + "-fx-background-color: lightyellow;" + "-fx-padding: 0.2em 1em 0.2em 0.5em;" + "-fx-background-radius: 3px;";

  private static final String        DATE_FORMAT_HELP      = """
      Symbol\tMeaning\t\t\t\t\tPresentation\tExamples
      --------\t---------\t\t\t\t\t--------------\t---------
       G\t\t era\t\t\t\t\t\t text\t\t\t AD; Anno Domini; A
       u\t\t year\t\t\t\t\t\t year\t\t\t 2004; 04
       y\t\t year-of-era\t\t\t\t year\t\t\t 2004; 04
       D\t\t day-of-year\t\t\t\t number\t\t 189
       M/L\t\t month-of-year\t\t\t number/text\t 7; 07; Jul; July; J
       d\t\t day-of-month\t\t\t\t number\t\t 10

       U\t\t half-of-year\t\t\t\t number/text\t 2; H2; 2nd half
       Q/q\t\t quarter-of-year\t\t\t number/text\t 3; 03; Q3; 3rd quarter
       Y\t\t week-based-year\t\t\t year\t\t\t 1996; 96
       w\t\t week-of-week-based-year\t number\t\t 27
       W\t\t week-of-month\t\t\t number\t\t 4
       E\t\t day-of-week\t\t\t\t text\t\t\t Tue; Tuesday; T
       e/c\t\t localized day-of-week\t\t number/text\t 2; 02; Tue; Tuesday; T
       F\t\t aligned-week-of-month\t\t number\t\t 3

       p\t\t pad next\t\t\t\t\t pad modifier\t 1
       '\t\t escape for text\t\t\t delimiter
       ''\t\t single quote\t\t\t\t literal\t\t '
        """;

  private static final String        DATE_TIME_FORMAT_HELP = """
      Symbol\tMeaning\t\t\t\t\tPresentation\tExamples
      --------\t---------\t\t\t\t\t--------------\t---------
       G\t\t era\t\t\t\t\t\t text\t\t\t AD; Anno Domini; A
       u\t\t year\t\t\t\t\t\t year\t\t\t 2004; 04
       y\t\t year-of-era\t\t\t\t year\t\t\t 2004; 04
       D\t\t day-of-year\t\t\t\t number\t\t 189
       M/L\t\t month-of-year\t\t\t number/text\t 7; 07; Jul; July; J
       d\t\t day-of-month\t\t\t\t number\t\t 10

       U\t\t half-of-year\t\t\t\t number/text\t 2; H2; 2nd half
       Q/q\t\t quarter-of-year\t\t\t number/text\t 3; 03; Q3; 3rd quarter
       Y\t\t week-based-year\t\t\t year\t\t\t 1996; 96
       w\t\t week-of-week-based-year\t number\t\t 27
       W\t\t week-of-month\t\t\t number\t\t 4
       E\t\t day-of-week\t\t\t\t text\t\t\t Tue; Tuesday; T
       e/c\t\t localized day-of-week\t\t number/text\t 2; 02; Tue; Tuesday; T
       F\t\t aligned-week-of-month\t\t number\t\t 3

       a\t\t am-pm-of-day\t\t\t text\t\t\t PM
       h\t\t clock-hour-of-am-pm (1-12)\t number\t\t 12
       K\t\t hour-of-am-pm (0-11)\t\t number\t\t 0
       k\t\t clock-hour-of-day (1-24)\t number\t\t 24

       H\t\t hour-of-day (0-24*)\t\t number\t\t 0
       m\t\t minute-of-hour\t\t\t number\t\t 30
       s\t\t second-of-minute\t\t\t number\t\t 55
       S\t\t fraction-of-second\t\t\t fraction\t\t 978
       A\t\t milli-of-day\t\t\t\t number\t\t 1234
       N\t\t nano-of-day\t\t\t\t number\t\t 1234000000

       p\t\t pad next\t\t\t\t\t pad modifier\t 1
       '\t\t escape for text\t\t\t delimiter
       ''\t\t single quote\t\t\t\t literal\t\t '

      * H can be 24 only for an explicit stored 24:00.
        """;

  // listener for non-tooltip popups (e.g. menus) when focus within grid
  private ListChangeListener<Window> m_popupListener       = change ->
                                                           {
                                                             while ( change.next() )
                                                               for ( Window window : change.getAddedSubList() )
                                                                 if ( window instanceof PopupWindow popup
                                                                     && !( popup instanceof Tooltip )
                                                                     && getScene() != null
                                                                     && popup.getOwnerWindow() == getScene().getWindow()
                                                                     && m_grid.isFocusWithin() )
                                                                   updatePlan( null );
                                                           };

  // record to hold plan values for undo/redo
  private record Values( String title, DateTime start, Calendar calendar, TimeSpan endRounding, String dateTimeFormat,
      String dateFormat )
  {
    private static Values from( Plan plan )
    {
      return new Values( plan.getTitle(), plan.getDefaultStart(), plan.getDefaultCalendar(),
          plan.getDefaultEndRounding(), plan.getDateTimeFormat(), plan.getDateFormat() );
    }

    private void applyTo( Plan plan )
    {
      plan.setTitle( title );
      plan.setDefaultStart( start );
      plan.setDefaultCalendar( calendar );
      plan.setDefaultEndRounding( endRounding );
      plan.setDateTimeFormat( dateTimeFormat );
      plan.setDateFormat( dateFormat );
    }
  }

  // undo-command for plan properties edits, which can be merged with consecutive edits
  private final class PropertiesCommand implements IUndoCommand
  {
    // keep the original values fixed while consecutive edits replace the latest values
    private final Values m_oldValues = Values.from( m_plan );
    private Values       m_newValues;

    private PropertiesCommand( Values values )
    {
      m_newValues = values;
      if ( isValid() )
        redo();
    }

    private void merge( Values values )
    {
      // fold a consecutive properties edit into this undo command
      m_newValues = values;
      redo();
    }

    @Override
    public void redo()
    {
      // restore the most recently committed property values
      m_newValues.applyTo( m_plan );
      updateFromPlan();
    }

    @Override
    public void undo()
    {
      // restore the values from before the first merged edit
      m_oldValues.applyTo( m_plan );
      updateFromPlan();
    }

    @Override
    public String text()
    {
      return "Plan properties";
    }

    @Override
    public boolean isValid()
    {
      return !m_oldValues.equals( m_newValues );
    }
  }

  /**************************************** constructor ******************************************/
  public PlanProperties( PlanContext context )
  {
    // remember plan and setup gui
    m_plan = context.getPlan();
    m_status = context.getStatus();
    m_undoStack = context.getUndoStack();
    m_numberOf = new NumberOf( m_plan );
    m_defaultCalendar = new ChooseCalendar( m_plan.getCalendars() );

    setup();
    updateFromPlan();
    m_status.clear();
  }

  /******************************************* setup *********************************************/
  private void setup()
  {
    // setup scrolling properties panel
    setMinWidth( 0.0 );
    setFitToWidth( true );
    setFitToHeight( true );

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

    // add field rows
    int row = 0;
    addRow( m_grid, row++, "Title", m_title, false );
    addRow( m_grid, row++, "Default start", m_defaultStart, false );
    addRow( m_grid, row++, "Actual start", m_actualStart, true );
    addRow( m_grid, row++, "End", m_end, true );
    addRow( m_grid, row++, "Default calendar", m_defaultCalendar, false );
    addRow( m_grid, row++, "Default rounding", m_defaultEndRounding, false );
    addRow( m_grid, row++, "Date-time format", m_dateTimeFormat, false );
    addRow( m_grid, row++, "Date format", m_dateFormat, false );
    addRow( m_grid, row++, "File name", m_fileName, true );
    addRow( m_grid, row++, "File location", m_fileLocation, true );
    addRow( m_grid, row++, "Saved by", m_savedBy, true );
    addRow( m_grid, row++, "Saved when", m_savedWhen, true );

    // add number of
    row++;
    m_grid.add( m_numberOf, 0, row, 2, 1 );
    GridPane.setHgrow( m_numberOf, Priority.ALWAYS );
    GridPane.setVgrow( m_numberOf, Priority.ALWAYS );

    addFormatHelp();
    Window.getWindows().addListener( new WeakListChangeListener<>( m_popupListener ) );
  }

  /******************************************** addRow *******************************************/
  private void addRow( GridPane grid, int row, String text, Region control, boolean readonly )
  {
    // add row to grid
    Label label = new Label( text );
    grid.addRow( row, label, control );
    GridPane.setHalignment( label, HPos.RIGHT );
    GridPane.setHgrow( control, Priority.ALWAYS );

    // if want control to be read-only, set un-editable and background
    if ( readonly && control instanceof TextField field )
    {
      field.setEditable( false );
      field.setBackground( READONLY );
    }

    if ( !readonly && control instanceof TextField field )
    {
      field.addEventHandler( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );
      field.focusedProperty().addListener( ( properties, oldFocus, newFocus ) ->
      {
        if ( oldFocus )
          updatePlan( field );
      } );
    }

    // attach the control to application status if has observable status
    if ( control instanceof IObservableStatus field )
      field.setStatus( m_status );
  }

  /*************************************** addFormatHelp ****************************************/
  private void addFormatHelp()
  {
    // add tooltip for date-time field
    var dateTimeHelp = new Tooltip( DATE_TIME_FORMAT_HELP );
    dateTimeHelp.setStyle( TOOLTIP_STYLE );
    dateTimeHelp.setShowDuration( Duration.INDEFINITE );
    m_dateTimeFormat.setTooltip( dateTimeHelp );

    // add listener to check date-time format whenever text changes
    m_dateTimeFormat.textProperty().addListener( ( property, oldText, newText ) ->
    {
      String err = checkDateTimeFormat();
      if ( err == null )
        m_status.update( Level.INFO, "Date-time format example: " + DateTime.now().format( newText ) );
      else
        m_status.update( Level.ERROR, "Date-time format error '" + err + "'" );
      m_dateTimeFormat.setStyle( m_status.getStyle() );
    } );

    // add listener to trigger text change format check when date-time format gets focus to show example
    m_dateTimeFormat.focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
      {
        var text = m_dateTimeFormat.getText();
        m_dateTimeFormat.setText( null );
        m_dateTimeFormat.setText( text );
      }
    } );

    // add tooltip for date field
    var dateHelp = new Tooltip( DATE_FORMAT_HELP );
    dateHelp.setStyle( TOOLTIP_STYLE );
    dateHelp.setShowDuration( Duration.INDEFINITE );
    m_dateFormat.setTooltip( dateHelp );

    // add listener to check date format whenever text changes
    m_dateFormat.textProperty().addListener( ( property, oldText, newText ) ->
    {
      String err = checkDateFormat();
      if ( err == null )
        m_status.update( Level.INFO, "Date format example: " + Date.now().format( newText ) );
      else
        m_status.update( Level.ERROR, "Date format error '" + err + "'" );
      m_dateFormat.setStyle( m_status.getStyle() );
    } );

    // add listener to trigger text change format check when date format gets focus to show example
    m_dateFormat.focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
      {
        var text = m_dateFormat.getText();
        m_dateFormat.setText( null );
        m_dateFormat.setText( text );
      }
    } );

    // add tooltip, text and focus change listeners for default end rounding field
    m_defaultEndRounding.setPromptText( "None" );
    m_defaultEndRounding
        .setTooltip( new Tooltip( "End rounding copied into new tasks. Existing tasks keep their setting.\r"
            + "Enter a time-span such as 15M or 1H; clear the field or enter None for no rounding." ) );
    m_defaultEndRounding.getTooltip().setStyle( TOOLTIP_STYLE );
    m_defaultEndRounding.textProperty()
        .addListener( ( property, oldText, newText ) -> showEndRoundingStatus( checkEndRounding() ) );
    m_defaultEndRounding.focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      // recheck this field on entry so an error from another property cannot hide its description
      if ( newFocus )
        showEndRoundingStatus( checkEndRounding() );
    } );
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
      if ( source == m_dateTimeFormat )
        source.setText( m_plan.getDateTimeFormat() );
      if ( source == m_dateFormat )
        source.setText( m_plan.getDateFormat() );
      if ( source == m_title )
        source.setText( m_plan.getTitle() );
      if ( source == m_defaultStart )
        displayDateTime( m_defaultStart, m_plan.getDefaultStart() );
      if ( source == m_defaultCalendar )
        source.setText( m_plan.getDefaultCalendar().getName() );
      if ( source == m_defaultEndRounding )
        m_defaultEndRounding.setValue( m_plan.getDefaultEndRounding() );
      source.selectRange( pos, pos );
      event.consume();
    }

    // if enter pressed, update plan
    if ( event.getCode() == KeyCode.ENTER )
    {
      int pos = source.getCaretPosition();
      updatePlan( source );
      source.selectRange( pos, pos );
      event.consume();
    }
  }

  /************************************* checkDateTimeFormat *************************************/
  private String checkDateTimeFormat()
  {
    // check date-time format is valid, return null if okay, else error message
    try
    {
      if ( m_dateTimeFormat.getText().length() < 1 )
        throw new NumberFormatException( "Invalid format" );

      DateTime.now().format( m_dateTimeFormat.getText() );
      return null;
    }
    catch ( Exception exception )
    {
      return exception.getMessage();
    }
  }

  /*************************************** checkDateFormat ***************************************/
  private String checkDateFormat()
  {
    // check date format is valid, return null if okay, else error message
    try
    {
      if ( m_dateFormat.getText().length() < 1 )
        throw new NumberFormatException( "Invalid format" );

      Date.now().format( m_dateFormat.getText() );
      return null;
    }
    catch ( Exception exception )
    {
      return exception.getMessage();
    }
  }

  /*************************************** checkEndRounding **************************************/
  private String checkEndRounding()
  {
    // check optional rounding is valid, return null if okay, else error message
    try
    {
      if ( !m_defaultEndRounding.isValueValid() )
        throw new IllegalArgumentException( "Invalid duration" );
      TimeSpan rounding = TimeSpan.parse( m_defaultEndRounding.getTimeSpan() );
      if ( rounding != null && rounding.getNumber() <= 0 )
        throw new IllegalArgumentException( "End rounding must be positive; use None for no rounding" );

      return null;
    }
    catch ( IllegalArgumentException exception )
    {
      return exception.getMessage();
    }
  }

  /************************************* showEndRoundingStatus ***********************************/
  private void showEndRoundingStatus( String err )
  {
    if ( err != null )
      m_status.update( Level.ERROR, err );
    else if ( m_defaultEndRounding.isFocused() )
    {
      // retain the duration description after validation while the field is being edited
      TimeSpan rounding = m_defaultEndRounding.getTimeSpan();
      m_status.update( Level.INFO, rounding == null ? "" : rounding.toStringLong() );
    }
    m_defaultEndRounding.setStyle( m_status.getStyle() );
  }

  /*************************************** displayDateTime ***************************************/
  private void displayDateTime( TextField field, DateTime dt )
  {
    // display date-time in field using plan date-time format, or clear if null
    if ( dt == null )
      field.setText( null );
    else
      field.setText( dt.format( m_plan.getDateTimeFormat() ) );
  }

  /**************************************** updateFromPlan ***************************************/
  public void updateFromPlan()
  {
    // update the gui property widgets with values from plan
    m_title.setText( m_plan.getTitle() );
    m_defaultCalendar.setSelected( m_plan.getDefaultCalendar() );
    m_defaultEndRounding.setValue( m_plan.getDefaultEndRounding() );
    m_dateTimeFormat.setText( m_plan.getDateTimeFormat() );
    m_dateFormat.setText( m_plan.getDateFormat() );
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
  public void updatePlan( TextField field )
  {
    // clear status to remove any message when field loses focus
    if ( field == null || !field.isFocused() )
      m_status.clear();

    // update plan with values from gui property widgets via undo command
    var rounding = checkEndRounding() == null ? m_defaultEndRounding.getTimeSpan() : m_plan.getDefaultEndRounding();
    var dtFormat = checkDateTimeFormat() == null ? m_dateTimeFormat.getText() : m_plan.getDateTimeFormat();
    var dFormat = checkDateFormat() == null ? m_dateFormat.getText() : m_plan.getDateFormat();

    var values = new Values( m_title.getText(), m_defaultStart.getDateTime(), m_defaultCalendar.getCalendar(), rounding,
        dtFormat, dFormat );

    if ( !m_undoStack.isClean() && m_undoStack.getUndoCommand() instanceof PropertiesCommand command )
    {
      // merge consecutive property edits into the latest undo command
      command.merge( values );
      m_undoStack.commandMerged();
    }
    else
    {
      // new undo command
      m_undoStack.push( new PropertiesCommand( values ) );
    }
  }

}
