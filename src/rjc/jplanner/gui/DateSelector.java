/**************************************************************************
 *  Copyright (C) 2017 by Richard Crook                                   *
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import rjc.jplanner.JPlanner;
import rjc.jplanner.model.Calendar;
import rjc.jplanner.model.Date;

/*************************************************************************************************/
/********************* Pop-up window to display date-time selection widgets **********************/
/*************************************************************************************************/

class DateSelector extends Popup
{
  private DateEditor          m_parent;
  private boolean             m_ignoreUpdates;

  private Pane                m_pane;              // contains the pop-up widgets
  private HBox                m_date;              // for entering month + year
  private Canvas              m_calendar;          // for picking date
  private HBox                m_buttons;           // for today + start + end buttons

  private SpinEditor          m_year;
  private MonthSpinEditor     m_month;
  private SpinEditor          m_epochDay;

  private Button              m_today;             // set date to today, without changing time
  private Button              m_start;             // set time to working start of day, without changing date
  private Button              m_end;               // set time to working end of day, without changing date
  private Button              m_forward;           // move date-time forward to next working period boundary
  private Button              m_back;              // move date-time back to next working period boundary

  private GraphicsContext     m_gc;
  private double              m_columnWidth;
  private double              m_rowHeight;
  private double              m_x;
  private double              m_y;

  private static final double PADDING       = 3.0;
  private static final double HEIGHT        = 23.0;
  private static final double SHADOW_RADIUS = 4.0;

  /**************************************** constructor ******************************************/
  public DateSelector( DateEditor parent )
  {
    // create pop-up window to display date-time selection widgets
    m_parent = parent;
    setAutoHide( true );
    setConsumeAutoHidingEvents( false );
    constructSelector();

    // add shadow
    DropShadow shadow = new DropShadow();
    shadow.setColor( Colors.FOCUSBLUE );
    shadow.setRadius( SHADOW_RADIUS );
    getScene().getRoot().setEffect( shadow );

    // toggle pop-up when parent button is pressed or F2 key is pressed
    m_parent.getButton().setOnMousePressed( event -> toggleSelector() );
    m_parent.setOnKeyPressed( event ->
    {
      if ( event.getCode() == KeyCode.F2 )
        toggleSelector();
    } );

    // ensure parent editor is editable when selector is hidden
    setOnHidden( event -> m_parent.setEditable( true ) );

    // keep parent editor and this selector synchronised
    m_parent.textProperty().addListener( ( observable, oldText, newText ) -> setDate( m_parent.getDate() ) );
    m_year.textProperty().addListener( ( observable, oldText, newText ) -> updateParent( m_year ) );
    m_month.textProperty().addListener( ( observable, oldText, newText ) -> updateParent( m_month ) );
  }

  /*************************************** updateParent ******************************************/
  private void updateParent( XTextField trigger )
  {
    // if editor not showing, return immediately not doing anything
    if ( !isShowing() || m_ignoreUpdates )
      return;

    // if triggering editor was month or year, update never visible epoch-day editor
    if ( trigger == m_month || trigger == m_year )
    {
      int year = m_year.getInteger();
      int month = m_month.getMonthNumber();
      int day = LocalDate.ofEpochDay( m_epochDay.getInteger() ).getDayOfMonth();

      YearMonth ym = YearMonth.of( year, month );
      int max = ym.lengthOfMonth();
      day = Math.min( day, max );
      if ( day > max )
        day = max;

      m_epochDay.setDouble( LocalDate.of( year, month, day ).toEpochDay() );
    }

    // update parent editor to reflect date-time shown in selector
    m_parent.setDate( getDate() );
  }

  /***************************************** setDate *****************************************/
  private void setDate( Date date )
  {
    // set selector to specified date-time
    if ( date == null )
      return;

    m_ignoreUpdates = true;
    m_epochDay.setInteger( date.getEpochday() );
    m_month.setMonth( Month.of( date.getMonth() ) );
    m_year.setInteger( date.getYear() );
    m_ignoreUpdates = false;
    drawCalendar();
  }

  /******************************************* getDate *******************************************/
  private Date getDate()
  {
    // return date shown by selector
    return new Date( m_epochDay.getInteger() );
  }

  /*************************************** toggleSelector ****************************************/
  private void toggleSelector()
  {
    // if selector open, hide, if hidden, open
    if ( isShowing() )
      hide();
    else
    {
      m_parent.setEditable( false );
      Point2D point = m_parent.localToScreen( 0.0, m_parent.getHeight() );
      show( m_parent, point.getX() - SHADOW_RADIUS + 1.0, point.getY() - SHADOW_RADIUS + 1.0 );
      arrangeSelector();
      m_calendar.requestFocus();
    }
  }

  /************************************** constructSelector **************************************/
  private void constructSelector()
  {
    // define background and border for the selector
    Background BACKGROUND = new Background( new BackgroundFill( Colors.GENERAL_BACKGROUND, null, null ) );
    Border BORDER = new Border( new BorderStroke( Colors.FOCUSBLUE, BorderStrokeStyle.SOLID, null, null ) );

    // padding around the widgets
    Insets INSETS = new Insets( PADDING - 2.0, PADDING - 1.0, PADDING, PADDING );

    // create spin editors for entering date month-year
    m_date = new HBox();
    m_date.setPadding( INSETS );
    m_date.setSpacing( PADDING );
    m_year = createSpinEditor( -999999, 999999, 80, "0000", null );
    m_month = new MonthSpinEditor();
    m_month.setPrefWidth( 121 );
    m_month.setMinHeight( HEIGHT );
    m_month.setMaxHeight( HEIGHT );
    m_month.setYearSpinEditor( m_year );
    m_date.getChildren().addAll( m_month, m_year );

    // this spin editor is never visible, just used to support wrapping
    m_epochDay = createSpinEditor( Integer.MIN_VALUE, Integer.MAX_VALUE, 0, "0", null );

    // create calendar canvas
    m_calendar = new Canvas();
    m_calendar.setHeight( 17.0 * 7.0 );
    m_calendar.setFocusTraversable( true );
    m_calendar.setOnKeyPressed( event -> calendarKeyPressed( event ) );
    m_calendar.setOnKeyTyped( event -> calendarKeyTyped( event ) );
    m_calendar.setOnMouseClicked( event -> calendarMouseClicked( event ) );
    m_calendar.focusedProperty().addListener( ( observable, oldF, newF ) ->
    {
      if ( newF.booleanValue() )
        m_calendar.setStyle( "-fx-effect: dropshadow(gaussian, #039ed3, 4, 0.75, 0, 0);" );
      else
        m_calendar.setStyle( "" );
    } );

    // create buttons
    m_buttons = new HBox();
    m_buttons.setPadding( INSETS );
    m_buttons.setSpacing( PADDING );
    m_today = new Button( "Today" );
    m_back = new Button( "<" );
    m_start = new Button( "Start" );
    m_end = new Button( "End" );
    m_forward = new Button( ">" );
    m_buttons.getChildren().addAll( m_today, m_back, m_start, m_end, m_forward );

    // add button actions
    m_today.setOnAction( event -> m_parent.setDate( Date.now() ) );

    // populate pane
    m_pane = new Pane();
    m_pane.getChildren().addAll( m_date, m_calendar, m_buttons );
    m_pane.setBackground( BACKGROUND );
    m_pane.setBorder( BORDER );
    getContent().add( m_pane );
  }

  /************************************* createSpinEditor ****************************************/
  private SpinEditor createSpinEditor( int minValue, int maxValue, int width, String format, SpinEditor wrap )
  {
    // create a spin-editor
    SpinEditor spin = new SpinEditor();
    spin.setRange( minValue, maxValue, 0 );
    spin.setPrefWidth( width );
    spin.setMinHeight( HEIGHT );
    spin.setMaxHeight( HEIGHT );
    spin.setFormat( format );
    spin.setWrapSpinEditor( wrap );
    return spin;
  }

  /*************************************** arrangeSelector ***************************************/
  private void arrangeSelector()
  {
    // position date
    double y = PADDING;
    m_date.relocate( 0.0, y );

    m_pane.autosize();
  }

  /**************************************** drawCalendar *****************************************/
  private void drawCalendar()
  {
    // only draw if selector is showing
    if ( !isShowing() )
      return;

    // draw calendar for month-year specified in the spin editors
    int month = m_month.getMonthNumber();
    int year = m_year.getInteger();
    LocalDate localdate = LocalDate.of( year, month, 1 );
    localdate = localdate.minusDays( localdate.getDayOfWeek().getValue() - 1 );
    LocalDate selecteddate = m_parent.getDate().localDate();

    // calculate calendar cell width & height
    double w = m_calendar.getWidth();
    double h = m_calendar.getHeight();
    m_columnWidth = Math.floor( w / 7.0 );
    m_rowHeight = Math.floor( h / 7.0 );

    // clear the calendar
    m_gc = m_calendar.getGraphicsContext2D();
    m_gc.clearRect( 0.0, 0.0, w, h );
    m_gc.setFontSmoothingType( FontSmoothingType.LCD );
    Calendar calendar = JPlanner.gui.getPropertiesPane().getCalendar();

    // draw the calendar day labels and day-of-month numbers
    for ( int row = 0; row < 7; row++ )
    {
      m_y = row * m_rowHeight;
      for ( int column = 0; column < 7; column++ )
      {
        m_x = column * m_columnWidth;

        if ( row == 0 )
        // in first row put day of week
        {
          DayOfWeek day = DayOfWeek.of( column + 1 );
          String label = day.getDisplayName( TextStyle.SHORT, Locale.getDefault() ).substring( 0, 2 );
          drawText( label, Color.BLACK, Color.BEIGE );
        }
        else
        // in other rows put day-of-month numbers
        {
          // numbers are black except gray for other months and red for today
          Color textColor = Color.BLACK;
          if ( localdate.getMonthValue() != month )
            textColor = Color.GRAY;
          if ( localdate.isEqual( selecteddate ) )
            textColor = Color.WHITE;
          if ( localdate.isEqual( LocalDate.now() ) )
            textColor = Color.RED;

          // number background colour is shade of gray to white depending of day work
          double work = calendar.getDay( new Date( localdate ) ).getWork();
          if ( work > 1.0 )
            work = 1.0;
          Color backColor = Color.gray( work / 10.0 + 0.9 );

          // select day is blue
          if ( localdate.isEqual( selecteddate ) )
            backColor = Colors.FOCUSBLUE;

          // draw number and move to next day
          drawText( localdate.getDayOfMonth(), textColor, backColor );
          localdate = localdate.plusDays( 1 );
        }
      }
    }

  }

  /****************************************** drawText *******************************************/
  private void drawText( Object text, Color textColor, Color backgroundColor )
  {
    // draw text centred in box defined by m_x, m_y, m_columnWidth, m_rowHeight
    m_gc.setFill( backgroundColor );
    m_gc.fillRect( m_x, m_y, m_columnWidth, m_rowHeight );

    Bounds bounds = new Text( text.toString() ).getLayoutBounds();
    double x = m_x + ( m_columnWidth - bounds.getWidth() ) / 2.0;
    double y = m_y + ( m_rowHeight - bounds.getHeight() ) / 2.0 - bounds.getMinY();

    m_gc.setFill( textColor );
    m_gc.fillText( text.toString(), x, y );
  }

  /************************************** calendarMouseClicked **************************************/
  private void calendarMouseClicked( MouseEvent event )
  {
    // update editor date with calendar date clicked
    int column = (int) ( event.getX() / m_columnWidth );
    int row = (int) ( event.getY() / m_rowHeight );
    if ( row < 1 )
      return;

    LocalDate ld = LocalDate.of( m_year.getInteger(), m_month.getMonthNumber(), 1 );
    ld = ld.minusDays( ld.getDayOfWeek().getValue() - 1 );
    ld = ld.plusDays( column + 7 * ( --row ) );

    Date dt = new Date( ld );
    m_parent.setDate( dt );
    m_calendar.requestFocus();
  }

  /************************************ calendarKeyPressed ***************************************/
  private void calendarKeyPressed( KeyEvent event )
  {
    // react to key presses
    boolean handled = true;
    switch ( event.getCode() )
    {
      case HOME:
        int day = getDate().getDayOfMonth();
        m_parent.setDate( getDate().plusDays( 1 - day ) );
        break;

      case END:
        boolean leap = Year.isLeap( m_year.getInteger() );
        int len = m_month.getMonth().length( leap );
        day = getDate().getDayOfMonth();
        m_parent.setDate( getDate().plusDays( len - day ) );
        break;

      case PAGE_UP:
        m_parent.setDate( getDate().plusDays( -28 ) );
        break;

      case PAGE_DOWN:
        m_parent.setDate( getDate().plusDays( 28 ) );
        break;

      case UP:
      case KP_UP:
        m_parent.setDate( getDate().plusDays( -7 ) );
        break;

      case DOWN:
      case KP_DOWN:
        m_parent.setDate( getDate().plusDays( 7 ) );
        break;

      case RIGHT:
      case KP_RIGHT:
        m_parent.setDate( getDate().plusDays( 1 ) );
        break;

      case LEFT:
      case KP_LEFT:
        m_parent.setDate( getDate().plusDays( -1 ) );
        break;

      case ENTER:
      case ESCAPE:
        handled = false;
        if ( isShowing() )
          toggleSelector();
        break;

      default:
        handled = false;
        break;
    }

    // if handled then consume
    if ( handled )
      event.consume();
  }

  /************************************ calendarKeyPressed ***************************************/
  private void calendarKeyTyped( KeyEvent event )
  {
    // reach to key typed
    char key = event.getCharacter().charAt( 0 );

    // if digit typed, move date forward until day-of-month contains typed digit
    if ( Character.isDigit( key ) )
    {
      Date date = getDate();
      do
        date.increment();
      while ( Integer.toString( date.getDayOfMonth() ).indexOf( key ) < 0 );
      m_parent.setDate( date );
    }

  }

}