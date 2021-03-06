package games.RubiksCube;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Hashtable;

import games.BoardVector;

/**
 * Class CubeState represents a certain cube state. It comes in four different types (member {@code type}), two
 * types for each value of {@link CubeConfig#cubeType}:
 * <ul>
 *     <li>if {@link CubeConfig#cubeType}==<b>POCKET</b>:
 *     <ul>
 * 			<li> <b>COLOR_P</b>: color representation of a 2x2x2 pocket cube
 * 			<li> <b>TRAFO_P</b>: transformation representation for a 2x2x2 pocket cube
 *     </ul>
 *     <li>if {@link CubeConfig#cubeType}==<b>RUBIKS</b>:
 *     <ul>
 * 			<li> <b>COLOR_R</b>: color representation of a 3x3x3 Rubik's cube
 * 			<li> <b>TRAFO_R</b>: transformation representation for a 3x3x3 Rubik's cube
 *     </ul>
 * </ul>
 * Its member {@link #fcol} has 24 or 48 elements for pocket and Rubik's cube, resp. 
 * It stores in the case of a <b>color representation</b> type in {@code fcol[i]} the <b>f</b>ace <b>col</b>or of
 * cubie face i. The color is one out of {0,1,2,3,4,5} for colors {w,b,o,y,g,r} = 
 * {white,blue,orange,yellow,green,red}. For transformation representation type TRAFO_* see below.
 * <p>
 * Its member {@link #sloc} has 24 or 48 elements for pocket and Rubik's cube, resp. 
 * It stores the <b>s</b>ticker <b>loc</b>ation: {@code sloc[i]} holds the location of the sticker which is at
 * face {@code i} for the solved cube.
 * <p>
 * Its member {@code lastTwist} stores the last twist action (U,L,F,D,R,B) performed on this cube
 * (ID if none or not known). For 2x2x2 pocket cube only U,L,F are needed and allowed.
 * <p>
 * Its member {@code twistSequence} stores the twist sequence needed for generating this 
 * state from the default cube {@code def}, e.g. "L2U1" for {@code def.LTw(2).UTw(1)}. <br>
 * ({@code twistSequence=""} if the twist sequence is not known, e.g. because CubeState was 
 * generated by color symmetry transformation) 
 * <p>
 * CubeState has member functions {@link #uTr(int)}, {@link #lTr(int)}, {@link #fTr(int)} for whole-cube 
 * rotations and {@link #UTw(int)}, {@link #LTw(int)}, {@link #FTw(int)},
 *               {@link #DTw(int)}, {@link #RTw(int)}, {@link #BTw(int)} for cube twists.
 * For 2x2x2 pocket cube only U,L,F-twists are needed and allowed.
 * <p>
 * More details:
 * <p>
 * Two CubeState objects are <b>equal</b>, if their {@code fcol[]} arrays have the same content. 
 * To get the corresponding sameness-behaviour when using {@link HashSet#add(Object)} 
 * and {@link Hashtable#get(Object)}, the functions {@link #equals(Object)} 
 * <b>AND</b> {@link #hashCode()} from class Object have to be 
 * overridden here.
 * <p>
 * If CubeState {@code trafo} is of <b>transformation representation</b> type <b>TRAFO_*</b>, then  
 * {@code trafo.fcol[i]} stores the location (number) of the parent cubie face.
 * That is, if {@code trafo}  is applied to another color-type CubeState cS, the color of cS.fcol[i] 
 * will become {@code cS.fcol[trafo.fcol[i]]}. See {@link #apply(CubeState)}.
 * <p> 
 * The numbering in {@link #fcol} and {@link #sloc} runs around the cube faces in the following order: U, L, F, D, R, B. 
 * Within the first three faces, we start at the wbo-cubie; within the last three faces we start
 * at the ygr-cubie. Within each face we march around in counter-clockwise
 * orientation. This gives for the 2x2x2 pocket cube the following numbering:
 * <pre>
 *       3  2				
 *       0  1				
 * 5  4  8 11 18 17 23 22
 * 6  7  9 10 19 16 20 21
 *      14 13				
 *      15 12				
 * </pre>
 * 
 * @author Wolfgang Konen, TH Koeln, 2018-2020
 */
abstract public class CubeState implements Serializable {
	
	public static enum Type {COLOR_P,COLOR_R,TRAFO_P,TRAFO_R}
	public static enum Twist {ID,U,L,F,D,R,B}

	/**
	 * We number the corners connected to the U (up) face with {a,b,c,d} in the order of the U face locations
	 * ({0,1,2,3} for 2x2x2 and {0,2,4,6} for 3x3x3 cube)
	 * and those connected to the D (down) face with {e,f,g,h} in the order of the D face locations
	 * ({12,13,14,15} for 2x2x2 and {24,26,28,30} for 3x3x3 cube).
	 * @see GameBoardCubeGui2x2
	 * @see GameBoardCubeGui3x3
	 */
	public static enum Cor {a,b,c,d,e,f,g,h}
	
	/**
	 * {@code fcol} is the face color array with 24 (<b>COLOR_P</b>) or 48 (<b>COLOR_R</b>) elements. <br> 
	 * {@code fcol[i]} holds the face color for cubie face (sticker) with number {@code i} (<b>COLOR_*</b>).<br>
	 * {@code fcol[i]} holds the parent location for cubie face (sticker) with number {@code i} (<b>TRAFO_*</b>).
	 */
	public int[] fcol; 
	
	/**
	 * {@code sloc} is the sticker location, an array with 24 (<b>COLOR_P</b>) or 48 (<b>COLOR_R</b>) elements. <br>
	 * {@code sloc[i]} holds the location of the sticker which is at face {@code i} for the solved cube.  
	 */
	public int[] sloc;

	Type type = Type.COLOR_P;
	Twist lastTwist = Twist.ID;
	int lastTimes = 0;
	String twistSeq = "";   // e.g. "L2U1" means that 
							//		(CubeState.makeCubeState()).LTw(2).UTw(1) 
							// produces this. ("": not known).
	int minTwists = -1;		// minimum number of twists needed to solve this state (-1: not known)
	
	/**
	 * change the version ID for serialization only if a newer version is no longer 
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 12L;

	/**
	 * {@code invF, invL, invU} are the <b>inverse</b> transformations for {@link #FTw()}, {@link #LTw()}, {@link #UTw()}.<br>
	 * That is, {@code fcol[invF[i]]} is the color which cubie face {@code i} gets after {@link #FTw()} transformation.
	 * [{@code invF, invL, invU} are generated via {@link CubeStateFactory#generateInverseTs()}.]
	 */
	protected static int[]
			invU = null, invL = null, invF = null,
		 	invD = null, invR = null, invB = null;

	/**
	 * {@code tforF, tforL, tforU} are the <b>forward</b> transformations needed in {@link #FTw()}, {@link #LTw()}, {@link #UTw()}.<br>
	 * That is, {@code tforF[i]} is the new location for a sticker which was in location {@code i} prior to a {@link #FTw()} transformation.<br>
	 * {@code tforF, tforL, tforU} are filled via method {@link #generateForwardTs()},
	 * which is called once at startup time from the constructor {@link ArenaTrainCube#ArenaTrainCube(String, boolean)}.
	 */
	private static int[]
			tforU = null, tforL = null, tforF = null,
			tforD = null, tforR = null, tforB = null;

	public CubeState() {
		// empty, just a stub for  derived classes
	}

	/**
	 * Construct a new cube of Type {@code type} in default (solved) state
	 * @param type	the cube type
	 */
	public CubeState(Type type) {
		this.type = type;
		switch(type) {
		case COLOR_P:
			assert (CubeConfig.cubeType== CubeConfig.CubeType.POCKET);
			this.fcol = new int[] {0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5};
			break;
		case COLOR_R:
			assert (CubeConfig.cubeType== CubeConfig.CubeType.RUBIKS);
			this.fcol = new int[] {0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,
								   3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5};
			break;
		case TRAFO_P:
			assert (CubeConfig.cubeType== CubeConfig.CubeType.POCKET);
			// fcol[i] holds the parent of location i under the trafo represented by this CubeState object.
			// Initially this trafo is the id transformation for the pocket cube.
			this.fcol = new int[24];
			for (int i=0; i<fcol.length; i++) this.fcol[i] = i;
			break;
		case TRAFO_R:
			assert (CubeConfig.cubeType== CubeConfig.CubeType.RUBIKS);
			// fcol[i] holds the parent of location i under the trafo represented by this CubeState object.
			// Initially this trafo is the id transformation for the Rubik's cube.
			this.fcol = new int[48];
			for (int i=0; i<fcol.length; i++) this.fcol[i] = i;
			break;
		}
		// sloc[i], the location of each face i, is in any case the default sequence 0,...,n where n=fcol.length
		this.sloc = new int[this.fcol.length];
		for (int i=0; i<sloc.length; i++) this.sloc[i] = i;
	}
	
	/**
	 * Copy constructor
	 */
	public CubeState(CubeState cs) {
		this.type = cs.type;
		this.lastTwist = cs.lastTwist;
		this.lastTimes = cs.lastTimes;
		this.twistSeq = cs.twistSeq;
		this.minTwists = cs.minTwists;
		this.fcol = cs.fcol.clone();
		this.sloc = cs.sloc.clone();
	}

	// this is now in CubeStateFactory.generateInverseTs() and in the derived classes, resp.
//	/**
//	 * generate the <b>inverse</b> transformations {@link #invF}, {@link #invL} and {@link #invU}.
//	 */
//	public static void generateInverseTs() {
//	}

	abstract protected void show_invF_invL_invU();


	/**
	 * generate the <b>forward</b> transformations {@link #tforF} (and similar) from {@link #invF} (and similar).
	 * <p>
	 * [{@link #invF} is generated via {@link CubeStateFactory#generateInverseTs()}.]
	 */
	public static void generateForwardTs() {
		tforU = new int[invF.length];
		tforL = new int[invF.length];
		tforF = new int[invF.length];
		tforD = new int[invF.length];
		tforR = new int[invF.length];
		tforB = new int[invF.length];
		// Since 3x applying F is F^3 = F^{-1}, it also holds that 3x applying F^{-1} gives F^9 = F.
		// So we can calculate tforF by 3x appling invF:
		for (int i=0; i<invF.length; i++) {
			tforU[i] = invU[invU[invU[i]]];
			tforL[i] = invL[invL[invL[i]]];
			tforF[i] = invF[invF[invF[i]]];
			tforD[i] = invD[invD[invD[i]]];
			tforR[i] = invR[invR[invR[i]]];
			tforB[i] = invB[invB[invB[i]]];
		}
	}

	//
	// The following methods are currently only valid for 2x2x2 Pocket Cube.
	// (They need to be generalized later to the 3x3x3 case.)
	//
	
	// TODO: The whole cube rotations need later to be extended to transform also this.sloc. But for the moment 
	// this is not needed, because whole-cube rotations come only into play if we use color symmetries.
	
	/**
	 * Whole-cube rotation 90� counter-clockwise around the u-face
	 */
	abstract protected CubeState uTr();

	/**
	 * Whole-cube rotation 90� counter-clockwise around the f-face
	 */
	abstract protected CubeState fTr();

	/**	
	 * Whole-cube rotation 90� counter-clockwise around the l-face
	 */
	protected CubeState lTr() {
		return this.fTr().uTr(3).fTr(3);
	}
	
	/**
	 * Whole-cube rotation, {@code times} * 90� counter-clockwise around the u-face
	 */
	public CubeState uTr(int times) {
		for (int i=0; i<times; i++) this.uTr();
		return this;
	}
	
	/**
	 * Whole-cube rotation, {@code times} * 90� counter-clockwise around the l-face
	 */
	public CubeState lTr(int times) {
		for (int i=0; i<times; i++) this.lTr();
		return this;
	}
	
	/**
	 * Whole-cube rotation, {@code times} * 90� counter-clockwise around the f-face
	 */
	public CubeState fTr(int times) {
		for (int i=0; i<times; i++) this.fTr();
		return this;
	}
	
	
	
	/**
	 * 	 Counter-clockwise twist of the U-face
	 */
	protected CubeState UTw() {
		int i;
		int[] tmp = this.fcol.clone();
		for (i=0; i<invU.length; i++) this.fcol[i] = tmp[invU[i]];
		tmp = this.sloc.clone();
		for (i=0; i<tforU.length; i++) this.sloc[i] = tforU[tmp[i]]; 
		return this;
	}
	
	/**
	 * 	 Counter-clockwise twist of the L-face
	 */
	protected CubeState LTw() {
		int i;
		int[] tmp = this.fcol.clone();
		for (i=0; i<invL.length; i++) this.fcol[i] = tmp[invL[i]];
		tmp = this.sloc.clone();
		for (i=0; i<tforL.length; i++) this.sloc[i] = tforL[tmp[i]]; 
		return this;
	}

	/**
	 * 	 Counter-clockwise twist of the F-face
	 */
	protected CubeState FTw() {
		int i;
		int[] tmp = this.fcol.clone();
		for (i=0; i<invF.length; i++) this.fcol[i] = tmp[invF[i]];
		tmp = this.sloc.clone();
		for (i=0; i<tforF.length; i++) this.sloc[i] = tforF[tmp[i]]; 
		return this;
	}

	/**
	 * 	 Counter-clockwise twist of the D-face
	 */
	protected CubeState DTw() {
		int i;
		int[] tmp = this.fcol.clone();
		for (i=0; i<invD.length; i++) this.fcol[i] = tmp[invD[i]];
		tmp = this.sloc.clone();
		for (i=0; i<tforD.length; i++) this.sloc[i] = tforD[tmp[i]];
		return this;
	}

	/**
	 * 	 Counter-clockwise twist of the R-face
	 */
	protected CubeState RTw() {
		int i;
		int[] tmp = this.fcol.clone();
		for (i=0; i<invR.length; i++) this.fcol[i] = tmp[invR[i]];
		tmp = this.sloc.clone();
		for (i=0; i<tforR.length; i++) this.sloc[i] = tforR[tmp[i]];
		return this;
	}

	/**
	 * 	 Counter-clockwise twist of the B-face
	 */
	protected CubeState BTw() {
		int i;
		int[] tmp = this.fcol.clone();
		for (i=0; i<invB.length; i++) this.fcol[i] = tmp[invB[i]];
		tmp = this.sloc.clone();
		for (i=0; i<tforB.length; i++) this.sloc[i] = tforB[tmp[i]];
		return this;
	}

	/**
	 * U-face twist, {@code times} * 90� counter-clockwise
	 */
	public CubeState UTw(int times) {
		for (int i=0; i<times; i++) this.UTw();
		this.twistSeq = this.twistSeq + "U"+times;
		this.lastTwist = Twist.U;
		this.lastTimes = times;
		return this;
	}
	
	/**
	 * L-face twist, {@code times} * 90� counter-clockwise
	 */
	public CubeState LTw(int times) {
		for (int i=0; i<times; i++) this.LTw();
		this.twistSeq = this.twistSeq + "L"+times;
		this.lastTwist = Twist.L;
		this.lastTimes = times;
		return this;
	}
	
	/**
	 * F-face twist, {@code times} * 90� counter-clockwise
	 */
	public CubeState FTw(int times) {
		for (int i=0; i<times; i++) this.FTw();
		this.twistSeq = this.twistSeq + "F"+times;
		this.lastTwist = Twist.F;
		this.lastTimes = times;
		return this;
	}

	/**
	 * U-face twist, {@code times} * 90� counter-clockwise
	 */
	public CubeState DTw(int times) {
		for (int i=0; i<times; i++) this.DTw();
		this.twistSeq = this.twistSeq + "D"+times;
		this.lastTwist = Twist.D;
		this.lastTimes = times;
		return this;
	}

	/**
	 * L-face twist, {@code times} * 90� counter-clockwise
	 */
	public CubeState RTw(int times) {
		for (int i=0; i<times; i++) this.RTw();
		this.twistSeq = this.twistSeq + "R"+times;
		this.lastTwist = Twist.R;
		this.lastTimes = times;
		return this;
	}

	/**
	 * F-face twist, {@code times} * 90� counter-clockwise
	 */
	public CubeState BTw(int times) {
		for (int i=0; i<times; i++) this.BTw();
		this.twistSeq = this.twistSeq + "B"+times;
		this.lastTwist = Twist.B;
		this.lastTimes = times;
		return this;
	}

	// CAUTION: This method is not yet extended for member sloc. But apply(trafo) is only needed in case
	// of color symmetries.
	/**
	 * Apply transformation {@code trafo} to this
	 * @param trafo a {@link CubeState} object of type TRAFO_P or TRAFO_R
	 * @return the transformed 'this'
	 */
	public CubeState apply(CubeState trafo) {
		assert(trafo.type==Type.TRAFO_P || this.type==Type.TRAFO_R) : "Wrong type in apply(trafo) !";
		int i;
		int[] tmp = this.fcol.clone();
		//for (i=0; i<fcol.length; i++) tmp[i] = this.fcol[trafo.fcol[i]];
		for (i=0; i<fcol.length; i++) this.fcol[i] = tmp[trafo.fcol[i]]; 
		return this;		
	}
	
	/**
	 * Apply color transformation cT to {@code this}. {@code this} has to be of 
	 * type COLOR_P or COLOR_R. 
	 * @param cT color transformation
	 * @return the transformed 'this'
	 */
	public CubeState applyCT(ColorTrafo cT) {
		assert(this.type==Type.COLOR_P || this.type==Type.COLOR_R) : "Wrong type in apply(cT) !";
		int[] tmp = this.fcol.clone();
		for (int i=0; i<fcol.length; i++) this.fcol[i] = cT.fcol[tmp[i]];
		return this;		
	}
	
	/**
	 * Locate the cubie with the colors of {@link CubieTriple} {@code tri} in {@code this}. 
	 * {@code this} has to be of type COLOR_P or COLOR_R.<br>
	 * [This method is only needed if we want to use color symmetries.]
	 *
	 * @param tri
	 * @return a {@link CubieTriple} whose member {@code loc} carries the location of the cubie with 
	 * 		   the colors of {@code tri}.
	 */
	abstract public CubieTriple locate(CubieTriple tri);

	/**
	 * There are four possible board vector types, depending on {@link CubeConfig#boardVecType}
	 * <ul> 
	 * <li> <b>CUBESTATE</b>: the face color array of the cube, i.e. member {@link #fcol}
	 * <li> <b>CUBEPLUSACTION</b>: like CUBESTATE, but with two more int values added: the ordinal of the last twist and 
	 * 		the number of quarter turns in this twist
	 * <li> <b>STICKER</b>: similar to the coding suggested by [McAleer2018], we track the location of S stickers when
	 * 		the cube is twisted away from its original position. We represent it as a SxS field (one-hot encoding).
	 * <li> <b>STICKER2</b>: similar to STICKER, we track the location of S stickers. We represent it as a 2xS field.
	 * </ul>
	 * For details see the implementations in {@link CubeState2x2} and {@link CubeState3x3}
	 *
	 * @return an int[] vector representing the 'board' state (= cube state)
	 */
	abstract public BoardVector getBoardVector();
	
	public CubeState clearLast() {
		this.lastTwist = Twist.ID;
		this.lastTimes = 0;
		return this;
	}
	
	public CubeState print() {
		System.out.println(this.toString());
		return this;
	}
	
	/** 
	 * Return a one-line string representing this object: Print a 
	 * 
	 */
	public String toString() {
		int isep = (this.type==Type.COLOR_P) ? 4 : 8;
		DecimalFormat form = new DecimalFormat("00");
		String s = "";
		switch(this.type) {
		case TRAFO_P: 
    		for (int i=0; i<fcol.length; i++) {
    			if (i%4==0) s = s + "|";
    			s = s + form.format(fcol[i]);
    		}
    		break;
    	case TRAFO_R:
			for (int i=0; i<fcol.length; i++) {
				if (i%isep==0) s = s + "|"; else s = s + ",";
				s = s + form.format(fcol[i]);
			}
			break;
		default:
    		for (int i=0; i<fcol.length; i++) {
    			if (i%isep==0) s = s + "|";
    			s = s + fcol[i];
    		}
    		break;
		}
		s = s + "|";  
		return s;	
	}
	
	public String getTwistSeq() {
		return this.twistSeq;
	}
	
	/**
	 * Check that {@code this.twistSeq} matches with {@code this.fcol}.
	 * 
	 * @return true, if applying {@code this.twistSeq} to the solved cube yields the  
	 * same cube state as stored in {@code this.fcol}.<br>
	 * If {@code this.twistSeq=""} (not known), then return always true. 
	 */
	public boolean assertTwistSequence() {
		CubeStateFactory csFactory = new CubeStateFactory();
		CubeState tst = csFactory.makeCubeState();
		Twist T=Twist.ID;
		int times;
		String tw = this.twistSeq;
		if (tw.equals("")) // 'this' is from color transformation, we cannot perform check:  
			return true;
//		if (twistSeq.equals("L2U1")) {
//			int dummy=1;
//		}
		while(tw.length()>0) {
			if (tw.startsWith("U")) T=Twist.U;
			if (tw.startsWith("L")) T=Twist.L;
			if (tw.startsWith("F")) T=Twist.F;
			if (tw.startsWith("D")) T=Twist.D;
			if (tw.startsWith("R")) T=Twist.R;
			if (tw.startsWith("B")) T=Twist.B;
			assert (tw.length()>1);
			times = (int)(tw.charAt(1)-48);
			assert (1<=times && times<=3);
			switch(T) {
				case U: tst.UTw(times); break;
				case L: tst.LTw(times); break;
				case F: tst.FTw(times); break;
				case D: tst.DTw(times); break;
				case R: tst.RTw(times); break;
				case B: tst.BTw(times); break;
			}
			tw =  (tw.length()>2) ? tw.substring(2,tw.length()) : "";				
		}
		return this.equals(tst);
	}
	
	/**
	 * Checks whether elements of members fcol, sloc and type are the same in {@code this} and {@code other}.
	 * (This differs from {@link Object#equals(Object)}, since the latter tests, whether 
	 * the objects are the same, not their content.)
	 */
	public boolean isEqual(CubeState other) {
		if (this.type!=other.type) return false;
		for (int i=0; i<fcol.length; i++) {
			if (this.fcol[i]!=other.fcol[i]) 
				return false;
		}     		
		for (int i=0; i<sloc.length; i++) {
			if (this.sloc[i]!=other.sloc[i]) 
				return false;
		}     		
		return true;
	}
	
	/**
	 * It is important that {@link Object#equals(Object)} is overwritten here, so that objects
	 * of class CubeState which have the same elements in fcol[] are considered as
	 * equal. The operation equals is the one that HashSet::add() relies on
	 * 
	 * @see #hashCode()
	 * @see CubeStateMap#countDifferentStates()
	 */
	@Override
	public boolean equals(Object other) {
		assert (other instanceof CubeState) : "Object other is not of class CubeState";
		return isEqual((CubeState) other);
	}
	
	/**
	 * Like with {@link CubeState#equals(Object)}, it is equally important that {@link Object#hashCode()} is overwritten here in such a way
	 * that it returns the same hash code for objects with the same content. 
	 * Since the equality check for inserting an object into a Set (HashSet) is based on 
	 * sameness of equals() AND hashCode() (!!)  
	 * <p> 
	 * See <a href="https://stackoverflow.com/questions/6187294/java-set-collection-override-equals-method/11577351">
	 *     https://stackoverflow.com/questions/6187294/java-set-collection-override-equals-method/11577351</a>
	 *     
	 * @see Object#hashCode()    
	 * @see #equals(Object)    
	 */
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
} // class CubeState
   


