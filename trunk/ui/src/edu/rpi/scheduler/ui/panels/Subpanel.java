/*
 *  Copyright (c) 2004, The University Scheduler Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the University Scheduler Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package edu.rpi.scheduler.ui.panels;

import edu.rpi.scheduler.ui.SchedulerPanel;
import edu.rpi.scheduler.ui.SchedulingSession;

import javax.swing.JPanel;

/**
 * Provides an interface for creating a "tab" or "subpanel" or "step" in the
 * user's schedule-generating process, like selecting courses or viewing
 * generated schedules.
 *
 * In general, the order of the calling of methods in this interface will
 * resemble:
 *
 * <OL>
 * <LI>The applet loads
 * <UL>
 * <LI>{@code setScheduler}
 * <LI>{@code setSchedulerPanel}
 * <LI>{@code init}
 * </UL>
 * <LI>The user progresses to the subpanel right before this one
 * <UL>
 * <LI>{@code getNextButtonText}
 * </UL>
 * <LI>The user clicks Next in that subpanel to progress to this subpanel
 * <UL>
 * <LI>{@code preEnter}
 * <LI>{@code entering}
 * </UL>
 * <LI>This subpanel is visible
 * <UL>
 * <LI>{@code canProgress}
 * </UL>
 * <LI>The user clicks Next in this subpanel to get to the next panel
 * <UL>
 * <LI>{@code progressing}
 * </UL>
 * <LI>The subpanel after this panel is visible
 * <UL>
 * <LI>{@code getPrevButtonText}
 * </UL>
 * </OL>
 */
public abstract class Subpanel extends JPanel {
    public abstract void setSession(SchedulingSession session);

    /**
     * Called to inform this subpanel what {@code SchedulerPanel} owns it.
     * @param panel the parent {@code SchedulerPanel}
     */
    public abstract void setSchedulerPanel(SchedulerPanel panel);

    /**
     * Called when this subpanel is currently being shown to determine whether
     * the user is able to progress past this panel (in practice, by clicking
     * the Next button in the bottom-right). If this method returns
     * {@code false}, the user will not be able to progress to the next
     * subpanel (but note that the user cannot be stopped from moving
     * backwards!).
     * @return whether the user can progress past this subpanel
     */
    public abstract boolean canProgress();

    /**
     * Called when this subpanel should initialize itself (namely, its UI). This
     * can be called at any time, any number of times, but in practice it is
     * called in the background to shorten the delay when the user clicks Next
     * to advance to each panel. Note that this method may also never be called.
     */
    public abstract void init();

    /**
     * When the user selects to progress to the next subpanel, like by clicking
     * the Next button, this method is called to determine whether or not the
     * subpanel should actually be switched. This is useful if you may not want
     * to show your panel (for example, if there are no possible schedules, for
     * the view-generated-schedules panel), but instead show a dialog box or
     * do something else interesting.
     *
     * If this method returns {@code false}, the current panel will stay
     * visible.
     * @return whether this panel should be shown
     */
    public abstract boolean preEnter();

    /**
     * Called before this panel is shown to the user.
     */
    public abstract void entering();

    /**
     * Called before another panel is switched to by the user, and this panel
     * will no longer be visible.
     */
    public abstract void progressing();

    /**
     * Returns the text that should be displayed to the user on a Next button
     * when the previous panel is being displayed. This should be a description
     * of this panel, like "Generate schedules."
     * @return the text to display to the user on the Next button when the panel
     *         before this one is visible to the user
     */
    public abstract String getNextButtonText();

    /**
     * Returns the text that should be displayed to the user on a Previous
     * button when the panel after this one is currently visible. This should
     * be a description of this panel, like "Select courses."
     * @return the text to display to the user on the Previous button when the
     *         panel after this one is visible to the user
     */
    public abstract String getPrevButtonText();

    public abstract void disposeSubpanel();

    public abstract void handleEnteringError(Throwable t);

    public abstract String getInfoBarText();

    public abstract String getPanelTitle();
}
