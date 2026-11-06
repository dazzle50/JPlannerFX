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

package rjc.jplanner.gui.gantt;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksView;
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.Colours;

/*************************************************************************************************/
/********************** Gantt shows tasks in a gantt plot with gantt scales **********************/
/*************************************************************************************************/

public class Gantt extends Region
{
  private ArrayList<GanttAxis> m_axes;                               // list of gantt-axes showing timeline
  private GanttNavigation      m_navigation;                         // horizontal navigation and undo
  private GanttPlot            m_plot;                               // gantt plot chart showing the tasks
  private GanttScrollBar       m_scrollBar;                          // horizontal scroll bar
  private PlanContext          m_context;                            // shared undo history for keyboard shortcuts
  private TasksView            m_tasksView;                          // table whose header and zoom align with the axes
  private GanttAxis            m_contextAxis;                        // last targeted axis for keyboard context menus
  private GanttAxisContextMenu m_contextMenu;                        // currently open axis menu, or null
  private boolean              m_fitPending;                         // view still needs its first fit
  private boolean              m_fitQueued;                          // fit is waiting for layout to finish

  private ObservableInteger    m_axesHeight;                         // height in pixels of all gantt axes

  final public static int      SCROLLBAR_SIZE   = 18;                // scroll bar height (thickness)
  final public static int      GANTTAXIS_HEIGHT = 15;                // gantt axis individual height

  static final double          MAX_LAYOUT_SIZE  = 999_999_999.0;     // maximum accepted layout dimension in pixels
  static final int             MAX_AXES         = 5;                 // maximum number of timeline axes

  // gantt colours
  public static final Color    GANTT_BACKGROUND = Color.WHITE;
  public static final Color    GANTT_NONWORKING = Color.gray( 0.95 );
  public static final Color    GANTT_DIVIDER    = Color.SILVER;
  public static final Color    GANTT_TASK_EDGE  = Color.BLACK;
  public static final Color    GANTT_TASK_FILL  = Color.YELLOW;
  public static final Color    GANTT_SUMMARY    = Color.BLACK;
  public static final Color    GANTT_MILESTONE  = Color.BLACK;
  public static final Color    GANTT_DEPENDENCY = Color.SLATEGRAY;
  public static final Color    GANTT_DEADLINE   = Color.GREEN;

  /**************************************** constructor ******************************************/
  public Gantt( TasksView view )
  {
    // construct the gantt
    m_tasksView = view;
    m_context = (PlanContext) view.getData().getUserData();
    m_fitPending = true;
    m_navigation = new GanttNavigation( m_context, this::getWidth, this::redraw );
    m_scrollBar = m_navigation.getScrollBar();
    var scale = m_navigation.getScale();
    m_axes = new ArrayList<>( 2 );
    m_axes.add( new GanttAxis( scale, GanttAxisInterval.MONTH ) );
    m_axes.add( new GanttAxis( scale, GanttAxisInterval.WEEK ) );
    m_contextAxis = m_axes.get( 0 );
    m_plot = new GanttPlot( view, scale );
    getChildren().addAll( m_axes.get( 0 ), m_axes.get( 1 ), m_plot, m_scrollBar );

    // allow keyboard navigation into the gantt and acquire focus from any part of it
    setFocusTraversable( true );
    addEventFilter( MouseEvent.MOUSE_PRESSED, event ->
    {
      requestFocus();
      if ( event.getTarget() instanceof GanttAxis axis )
        m_contextAxis = axis;
    } );
    addEventFilter( KeyEvent.KEY_PRESSED, this::keyPressed );
    setOnContextMenuRequested( this::openContextMenu );

    // detach shared data listeners while hidden, then refresh the range when shown again
    IListener dataListener = ( sender, message ) ->
    {
      m_navigation.invalidateRange();
      requestLayout();
    };
    IListener zoomListener = ( sender, message ) ->
    {
      // table zoom changes vertical geometry without changing the horizontal date scale
      updateAxesGeometry();
      requestLayout();
    };
    IListener geometryListener = ( sender, message ) ->
    {
      // restore the shared header height after the table's Reset view action
      if ( view.getRowsAxis().getHeaderPixels() != m_axesHeight.get() )
        updateAxes();
    };
    ISignal[] sources = { view.getData(), view.getData().rowCountProperty() };
    sceneProperty().addListener( ( observable, oldScene, newScene ) ->
    {
      for ( ISignal source : sources )
      {
        if ( oldScene != null )
          source.removeListener( dataListener );
        if ( newScene != null )
          source.addListener( dataListener );
      }
      if ( oldScene != null )
      {
        if ( m_contextMenu != null )
          m_contextMenu.hide();
        view.getZoom().removeListener( zoomListener );
        view.getRowsAxis().getTotalPixelsProperty().removeListener( geometryListener );
      }
      if ( newScene != null )
      {
        view.getZoom().addListener( zoomListener );
        view.getRowsAxis().getTotalPixelsProperty().addLaterListener( geometryListener );
        updateAxes();
        m_navigation.invalidateRange();
        requestLayout();
      }
    } );

    // property containing height in pixels of all gantt axes (for 'Tasks' table-view header height)
    m_axesHeight = new ObservableInteger();
    updateAxes();

    // set background colour to same as normal table-view header background colour
    setBackground( new Background( new BackgroundFill( Colours.HEADER_DEFAULT_FILL, null, null ) ) );
  }

  /*************************************** openContextMenu ***************************************/
  private void openContextMenu( ContextMenuEvent event )
  {
    // open the context menu for the last targeted axis, or the first axis if none has been targeted yet
    GanttAxis axis = event.getTarget() instanceof GanttAxis target ? target : null;
    if ( event.isKeyboardTrigger() )
      axis = m_contextAxis;
    if ( axis == null || !m_axes.contains( axis ) )
      return;

    m_contextAxis = axis;

    // replace the previous menu for repeated mouse or keyboard requests
    if ( m_contextMenu != null )
      m_contextMenu.hide();
    var menu = new GanttAxisContextMenu( this, axis );
    menu.setOnHidden( hidden ->
    {
      if ( m_contextMenu == menu )
        m_contextMenu = null;
    } );
    m_contextMenu = menu;

    // window ownership lets clicks anywhere in the Gantt dismiss the menu normally
    menu.show( getScene().getWindow(), event.getScreenX(), event.getScreenY() );
    event.consume();
  }

  /**************************************** getAxisCount *****************************************/
  int getAxisCount()
  {
    // return the number of gantt axes currently in use
    return m_axes.size();
  }

  /****************************************** addAxis ********************************************/
  void addAxis( GanttAxis relative, boolean above, GanttAxisInterval interval, GanttAxisLabelFormat format )
  {
    // add a new axis above or below the specified relative axis, or at the end if relative is null
    int index = m_axes.indexOf( relative );
    if ( index < 0 || interval == null || format == null || !format.isRelevantTo( interval )
        || m_axes.size() >= MAX_AXES )
      return;

    // apply both settings before insertion so addition remains a single undoable action
    var axis = new GanttAxis( m_navigation.getScale(), interval );
    axis.setLabelFormat( format );
    var command = new AxisListCommand( axis, index + ( above ? 0 : 1 ), true );
    command.redo();
    m_context.getUndoStack().push( command );
  }

  /**************************************** removeAxis *******************************************/
  void removeAxis( GanttAxis axis )
  {
    // remove the specified axis from the gantt, if it is not the last remaining axis
    int index = m_axes.indexOf( axis );
    if ( index < 0 || m_axes.size() <= 1 )
      return;

    var command = new AxisListCommand( axis, index, false );
    command.redo();
    m_context.getUndoStack().push( command );
  }

  /*************************************** setAxisSettings ***************************************/
  void setAxisSettings( GanttAxis axis, GanttAxisInterval interval, GanttAxisLabelFormat format )
  {
    // validate the pair before changing either value to keep every edit and undo position consistent
    if ( !m_axes.contains( axis ) || interval == null || format == null || !format.isRelevantTo( interval )
        || axis.getInterval() == interval && axis.getLabelFormat() == format )
      return;

    var command = new AxisSettingsCommand( axis, interval, format );
    command.redo();
    m_context.getUndoStack().push( command );
  }

  /***************************************** applyAxis *******************************************/
  private void applyAxis( GanttAxis axis, int index, boolean add )
  {
    // retain the same axis objects and insertion positions across undo and redo
    if ( add )
    {
      m_axes.add( index, axis );
      getChildren().add( index, axis );
    }
    else
    {
      m_axes.remove( index );
      getChildren().remove( axis );
      if ( m_contextAxis == axis )
        m_contextAxis = m_axes.get( Math.min( index, m_axes.size() - 1 ) );
    }
    updateAxes();
  }

  /**************************************** updateAxes *******************************************/
  private void updateAxes()
  {
    // update table geometry before notifying consumers of the combined pixel height
    m_tasksView.getRowsAxis().setHeaderNominalSize( m_axes.size() * GANTTAXIS_HEIGHT );
    updateAxesGeometry();
    m_tasksView.updateLayout();
    m_tasksView.redraw();
    requestLayout();
  }

  /************************************* updateAxesGeometry **************************************/
  private void updateAxesGeometry()
  {
    // update the geometry of all gantt axes to match the current table zoom and row height
    double zoom = m_tasksView.getZoom().get();
    int y = 0;
    for ( int index = 0; index < m_axes.size(); index++ )
    {
      // round cumulative boundaries exactly as the table rounds its combined header height
      int end = (int) ( ( index + 1 ) * GANTTAXIS_HEIGHT * zoom );
      var axis = m_axes.get( index );
      axis.setLayoutY( y );
      axis.setHeight( end - y );
      axis.setZoom( zoom );
      y = end;
    }
    m_axesHeight.set( y );
  }

  /***************************************** keyPressed ******************************************/
  private void keyPressed( KeyEvent event )
  {
    // pass key event to navigation for undo/redo and horizontal scrolling
    if ( !event.isControlDown() || event.isAltDown() || event.isMetaDown() || getWidth() <= 0.0 )
      return;

    switch ( event.getCode() )
    {
      case PLUS, EQUALS, ADD -> m_navigation.zoomIn();
      case MINUS, SUBTRACT -> m_navigation.zoomOut();
      case DIGIT0, NUMPAD0 -> m_navigation.fit( true );
      case Z -> m_context.getUndoStack().undo();
      case Y -> m_context.getUndoStack().redo();
      default -> {
        return;
      }
    }
    event.consume();
  }

  /******************************************* redraw ********************************************/
  private void redraw()
  {
    // redraw the gantt plot and axes
    for ( var axis : m_axes )
      axis.redraw();
    m_plot.redraw();
  }

  /*************************************** layoutChildren ****************************************/
  @Override
  protected void layoutChildren()
  {
    // skip layout if dimensions not ready
    double w = getWidth();
    double h = getHeight();
    if ( w <= 0.0 || h <= 0.0 || w > MAX_LAYOUT_SIZE || h > MAX_LAYOUT_SIZE )
      return;

    m_navigation.refresh();

    // layout gantt axes at top
    updateAxesGeometry();
    for ( var axis : m_axes )
      axis.setWidth( w );
    int y = m_axesHeight.get();

    // layout gantt plot below axes
    m_plot.setWidth( w );
    int plotHeight = (int) ( h - m_scrollBar.getHeight() - y );
    if ( plotHeight < 0 )
      plotHeight = 0;
    m_plot.setHeight( plotHeight );
    m_plot.setLayoutY( y );

    // layout gantt scroll-bar below plot
    m_scrollBar.setPrefWidth( w );
    m_scrollBar.setLayoutY( y + plotHeight );
    super.layoutChildren();

    // initialise each view after the split pane has finished sizing, even if the tab was initially hidden
    if ( m_fitPending && !m_fitQueued && m_navigation.canFit() && getScene() != null )
    {
      m_fitQueued = true;
      Platform.runLater( () ->
      {
        m_fitQueued = false;
        if ( getScene() != null && m_navigation.canFit() )
        {
          m_fitPending = false;
          m_navigation.fit( false );
        }
      } );
    }
  }

  /************************************* axesHeightProperty **************************************/
  public ReadOnlyInteger axesHeightProperty()
  {
    // return read-only property for axes height in pixels
    return m_axesHeight.getReadOnly();
  }

  // one insertion or removal retains only its target, position and direction
  private class AxisListCommand implements IUndoCommand
  {
    private final GanttAxis m_axis;  // same axis restored by undo and redo
    private final int       m_index; // insertion position in both ordered child lists
    private final boolean   m_add;   // whether redo adds this axis

    /**************************************** constructor ****************************************/
    AxisListCommand( GanttAxis axis, int index, boolean add )
    {
      m_axis = axis;
      m_index = index;
      m_add = add;
    }

    /******************************************* redo ********************************************/
    @Override
    public void redo()
    {
      applyAxis( m_axis, m_index, m_add );
    }

    /******************************************* undo ********************************************/
    @Override
    public void undo()
    {
      applyAxis( m_axis, m_index, !m_add );
    }

    /******************************************* text ********************************************/
    @Override
    public String text()
    {
      return m_add ? "Add Gantt axis" : "Remove Gantt axis";
    }
  }

  // interval and format selections form one edit and restore both original values on undo
  private class AxisSettingsCommand implements IUndoCommand
  {
    private final GanttAxis            m_axis;        // axis whose interval and labels are changed
    private final GanttAxisInterval    m_oldInterval; // original interval for undo
    private final GanttAxisInterval    m_newInterval; // selected interval for redo
    private final GanttAxisLabelFormat m_oldFormat;   // original format for undo
    private final GanttAxisLabelFormat m_newFormat;   // selected format for redo

    /**************************************** constructor ****************************************/
    AxisSettingsCommand( GanttAxis axis, GanttAxisInterval interval, GanttAxisLabelFormat format )
    {
      m_axis = axis;
      m_oldInterval = axis.getInterval();
      m_newInterval = interval;
      m_oldFormat = axis.getLabelFormat();
      m_newFormat = format;
    }

    /******************************************* redo ********************************************/
    @Override
    public void redo()
    {
      m_axis.setInterval( m_newInterval );
      m_axis.setLabelFormat( m_newFormat );
    }

    /******************************************* undo ********************************************/
    @Override
    public void undo()
    {
      m_axis.setInterval( m_oldInterval );
      m_axis.setLabelFormat( m_oldFormat );
    }

    /******************************************* text ********************************************/
    @Override
    public String text()
    {
      return "Gantt axis " + m_newInterval.getText() + " " + m_newFormat.getText();
    }
  }
}
