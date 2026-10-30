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

import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.days.DayType;
import rjc.table.data.types.Date;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/*********************** Dialog for editing individual calendar exceptions ***********************/
/*************************************************************************************************/

public class CalendarExceptionsDialog extends Dialog<Map<Date, DayType>>
{
  private final CalendarExceptionsView m_view;                  // table with date and day-type editors
  private final Label                  m_message = new Label(); // date information and validation feedback

  /**************************************** constructor ******************************************/
  public CalendarExceptionsDialog( Window owner, Plan plan, Calendar calendar )
  {
    var draft = new HashMap<>( calendar.getExceptions() );
    var data = new CalendarExceptionsData( draft, plan.getDayTypes() );

    m_view = new CalendarExceptionsView( data, calendar, plan.getDayTypes() );
    m_view.setStatus( new ObservableStatus() );

    initOwner( owner );
    initModality( Modality.WINDOW_MODAL );
    setTitle( "Calendar exceptions - " + calendar.getName() );
    setResizable( true );

    getDialogPane().getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );
    getDialogPane().setContent( createContent() );

    m_view.getStatus().addListener( ( sender, message ) -> updateMessage() );
    updateMessage();

    var ok = (Button) getDialogPane().lookupButton( ButtonType.OK );
    var cancel = (Button) getDialogPane().lookupButton( ButtonType.CANCEL );
    ok.addEventFilter( ActionEvent.ACTION, event ->
    {
      if ( !m_view.finishEditing() )
      {
        event.consume();
        return;
      }

      String error = plan.getCalendars().setValue( plan.getCalendars().indexOf( calendar ),
          Calendar.FIELD.Exceptions.ordinal(), draft, false );
      if ( error != null )
      {
        m_view.getStatus().update( Level.ERROR, error );
        event.consume();
      }
    } );
    setOnCloseRequest( event -> m_view.cancelEditing() );
    setResultConverter( button -> button == ButtonType.OK ? new HashMap<>( draft ) : null );

    // active cell editors handle their own Enter/Escape; otherwise these keys operate the dialog
    m_view.addEventFilter( KeyEvent.KEY_PRESSED, event ->
    {
      if ( m_view.isEditing() )
        return;
      if ( event.getCode() == KeyCode.ESCAPE )
        cancel.fire();
      else if ( event.getCode() == KeyCode.ENTER )
        ok.fire();
      else if ( event.getCode() == KeyCode.TAB )
        ( event.isShiftDown() ? cancel : ok ).requestFocus();
      else
        return;
      event.consume();
    } );

    m_view.selectDate( data.getDate( 0 ), 0 );
    setOnShown( event -> Platform.runLater( () -> m_view.requestFocus() ) );
  }

  /**************************************** createContent ****************************************/
  private VBox createContent()
  {
    // create a VBox with the table and message label
    var list = new StackPane( m_view );
    list.setPrefHeight( 200 );
    list.setPrefWidth( 400 );
    VBox.setVgrow( list, Priority.ALWAYS );

    // listen to list size & view zoom
    list.widthProperty().addListener( ( _, _, _ ) -> resizeColumn( list ) );
    m_view.getZoom().addListener( ( _, _ ) -> resizeColumn( list ) );
    m_view.getVerticalScrollBar().visibleProperty().addListener( ( _, _, _ ) -> resizeColumn( list ) );

    m_message.managedProperty().bind( m_message.visibleProperty() );
    m_message.setPadding( new Insets( 5, 10, 0, 10 ) );

    var vbox = new VBox( list, m_message );
    vbox.setPadding( Insets.EMPTY );
    return vbox;
  }

  /**************************************** resizeColumn *****************************************/
  private void resizeColumn( StackPane list )
  {
    // resize the day-type column to fill the remaining width of the pane
    var axis = m_view.getColumnsAxis();
    var width = list.getWidth() - axis.getPixelStart( 1, 0 );
    width -= m_view.getVerticalScrollBar().isVisible() ? m_view.getVerticalScrollBar().getWidth() : 0;

    axis.setNominalSize( 1, (int) ( width / m_view.getZoom().get() ) );
    m_view.redraw();
  }

  /**************************************** updateMessage ****************************************/
  private void updateMessage()
  {
    // display date information and validation feedback using the table view status style
    var status = m_view.getStatus();
    String message = status.getMessage();
    m_message.setText( message );
    m_message.setStyle( status.getStyle() );
    m_message.setVisible( message != null && !message.isEmpty() );
  }
}
