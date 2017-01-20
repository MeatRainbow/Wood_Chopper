/**
 * https://osbot.org/api/
 */

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.BooleanSupplier;


@ScriptManifest(author = "LRDBLK", info = "Chops wood", name = "Wood Chopper", version = 1, logo = "")
public class WoodCutter extends Script {

    /////////////
    //VARIABLES//
    ////////////

    private final Area LUM_TREES = new Area(3176, 3238, 3200, 3207);
    private final Area LUM_BANK = Banks.LUMBRIDGE_UPPER;

    private boolean shouldStart = false; //start script after button press
    private boolean shouldBank = true; //should script bank

    //script state
    private enum State{
        CHOP, BANK, TRAVEL_TO, TRAVEL_BACK, WAIT, DROP
    }

    //tree info for GUI
    public enum TreeInfo{

        TREE("Tree",  "Lumbridge"),
        OAK("Oak",  "Varock East", "Varock West"),
        WILLOW("Willow", "Barbarian Village", "Draynor Village");

        private String treeType;
        private String[] treeLocation;

        TreeInfo(String s, String ... l){

            treeType = s;
            treeLocation = l;
        }

        public String getTreeType(){
            return treeType;

        }

        public String[] getTreeLocation(){
            return treeLocation;
        }

    }

    /////////////
    //ACCESSORS//
    ////////////

    public boolean getShouldBank() {
        return shouldBank;
    }

    ////////////
    //MUTATORS//
    ///////////

    public void setShouldStart(boolean shouldStart) {
        this.shouldStart = shouldStart;
    }

    public void setShouldBank(boolean shouldBank) {
        this.shouldBank = shouldBank;
    }


    //////////////////////
    // REQUIRED METHODS //
    //////////////////////


    @Override
    public void onStart() {
        GuiMain gui = new GuiMain(this);
        gui.setVisible(true);
        getExperienceTracker().start(Skill.WOODCUTTING);
        log("Script is starting!");

    }

    @Override
    public int onLoop() throws InterruptedException {
        if (shouldStart) {
            switch(getState()){
                case CHOP:
                    RS2Object tree = getObjects().closest(LUM_TREES,"Tree");
                    chopTree(tree);
                    break;

                case TRAVEL_TO:
                    walking.webWalk(LUM_BANK);
                    break;

                case BANK:
                    log("depositing all");
                    depositBank();
                    break;

                case TRAVEL_BACK:
                    walking.webWalk(LUM_TREES);
                    break;

                case DROP:
                    log("dropping everything");
                    inventory.dropAll();
                    break;

                case WAIT:
                    getMouse().moveOutsideScreen();
                    sleep(1500, 500, () -> !working());
            }
        }

        return random(200, 300);
    }

    @Override
    public void onExit() {
        log("RawR");
    }

    @Override
    public void onPaint(Graphics2D g) {

        Point mP = getMouse().getPosition();

        //mouse X
        g.drawLine(mP.x - 5, mP.y + 5, mP.x + 5, mP.y - 5);
        g.drawLine(mP.x + 5, mP.y + 5, mP.x - 5, mP.y - 5);

        g.setColor(Color.white);

        Font normal = new Font("SANS_SERIF", Font.BOLD, 14);
        Font italic = new Font("SANS_SERIF", Font.ITALIC, 12);
        g.setColor(Color.WHITE);
        g.setFont(normal);

        g.drawString("LRDBLK's Wood Cutter !", 8, 30);
        g.drawString("Time Elapsed: " + formatTime(experienceTracker.getElapsed(Skill.WOODCUTTING)), 8, 45);
        g.drawString("Woodcutting Exp Gained: " + experienceTracker.getGainedXP(Skill.WOODCUTTING), 8, 60);
        g.setFont(italic);
        g.drawString(this.getState().toString(), 8, 90);



    }



    //////////////////////
    // CUSTOM METHODS //
    //////////////////////

    public String[] findTreeLocation(String s){
        for (TreeInfo t : TreeInfo.values()){
            if(t.getTreeType().contains(s)){
                return t.getTreeLocation();
            }
        }
        return null;
    }

    public String[] getTrees(){
        LinkedList l = new LinkedList();
        for(TreeInfo t : TreeInfo.values()){
            l.add(t.getTreeType());
        }
        return Arrays.copyOf(l.toArray(), l.toArray().length, String[].class);
    }

    private String formatTime(final long ms){
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        s %= 60; m %= 60; h %= 24;

        return d > 0 ? String.format("%02d:%02d:%02d:%02d", d, h, m, s) :
                h > 0 ? String.format("%02d:%02d:%02d", h, m, s) :
                        String.format("%02d:%02d", m, s);
    }

    /*
        Usage: get the current state of the player
     */
    private State getState(){
        RS2Object tree = getObjects().closest(LUM_TREES,"Tree");
        if (shouldBank) {
            if(inventory.isFull() && !LUM_BANK.contains(myPlayer())){
                return State.TRAVEL_TO;
            }else if(inventory.isFull() && LUM_BANK.contains(myPlayer())){
                return State.BANK;
            }else if(!LUM_TREES.contains(myPlayer())){
                return State.TRAVEL_BACK;
            }else if(tree != null && !working()){
                return State.CHOP;
            }
        }else{
            if (inventory.isFull()){
                return State.DROP;
            }else if( tree != null && !working()){
                return State.CHOP;
            }
        }

        return State.WAIT;
    }

    /*
        Usage: checks if player is able to start a new action
     */
    private boolean working(){
        return myPlayer().isAnimating() || myPlayer().isMoving();
    }


    /*
        Usage: Clicks chop down on a tree
        Argument: a tree Object
     */
    private void chopTree(RS2Object tree){

        int gainedXP = getExperienceTracker().getGainedXP(Skill.WOODCUTTING);
        log("Picked new tree!");
        tree.interact("Chop down");
        getMouse().moveRandomly();
        sleep(5000, 389, () -> (gainedXP != getExperienceTracker().getGainedXP(Skill.WOODCUTTING)) || !tree.exists());

    }

    /*
        Usage: sleeps for a certain amount of time using a given condition
        Arguments: maxSleep - max sleep time, deviation - +/- time, condition - when to stop sleeping
     */
    private static void sleep(int maxSleep, int deviation, BooleanSupplier condition) {
        new ConditionalSleep(maxSleep, deviation) {
            public boolean condition() throws InterruptedException {
                return condition.getAsBoolean();
            }
        }.sleep();
    }

    /*
        Usage: opens bank, deposits everything
     */
    private void depositBank() throws InterruptedException{

        log("banking!");
        if(!getBank().isOpen()){
            getBank().open();
            sleep(3000, 500, () -> getBank().isOpen());
        }else{
            getBank().depositAll();

        }

    }



}