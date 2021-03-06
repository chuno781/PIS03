package com.PrincipalPack;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.andengine.engine.Engine;
import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.SpriteBackground;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.color.Color;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;

import android.widget.Toast;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;

public class Game extends SimpleBaseGameActivity implements Serializable{
	/* The categories. */
	public static final short CATEGORYBIT_WALL = 1;
	public static final short CATEGORYBIT_CHARACTER = 2;
	public static final short CATEGORYBIT_ZERO = 4;

	/* And what should collide with what. */
	public static final short MASKBITS_WALL = CATEGORYBIT_WALL + CATEGORYBIT_CHARACTER+CATEGORYBIT_ZERO;
	public static final short MASKBITS_CHARACTER = CATEGORYBIT_WALL + CATEGORYBIT_CHARACTER+CATEGORYBIT_ZERO; // Missing: CATEGORYBIT_CIRCLE
	public static final short MASKBITS_ZERO= CATEGORYBIT_WALL + CATEGORYBIT_CHARACTER; // Missing: CATEGORYBIT_BOX

	public final FixtureDef WALL_FIXTURE_DEF = PhysicsFactory.createFixtureDef(10.0f, 0.5f, 1.0f, false, CATEGORYBIT_WALL, MASKBITS_WALL, (short)0);
	public final FixtureDef CHARACTER_FIXTURE_DEF = PhysicsFactory.createFixtureDef(1.0f, 1.0f, 1.0f, false, CATEGORYBIT_CHARACTER, MASKBITS_CHARACTER, (short)0);
	public final FixtureDef ZERO_FIXTURE_DEF = PhysicsFactory.createFixtureDef(0.0f, 0.0f, 0.0f, true, CATEGORYBIT_ZERO, MASKBITS_ZERO, (short)0);
	private int CAMERA_WIDTH;
	private int CAMERA_HEIGHT;
	// WHAT TO SAVE:
	protected BoundCamera mBoundChaseCamera;
	protected Scene mMainScene;
	protected volatile Map mMap;
	protected volatile Player mPlayer;
	String mapaName="tmx/bosque1.tmx";
	Maps mapas=new Maps();
	//
	public void save(){
		if (mPlayer.isFollowed()){
			Toast(R.string.CantSave);
		}else{
			String FILENAME = "AutoSave.yow";
			FileOutputStream fos;
			try {
				fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(new Save(mBoundChaseCamera,mMainScene,mMap,mPlayer,mapaName,mapas));
				oos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void load(){
		String FILENAME = "AutoSave.yow";
		FileInputStream fis;
		try {
			fis = openFileInput(FILENAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Save save=(Save) ois.readObject();
			mBoundChaseCamera=save.getmBoundChaseCamera();
			mMainScene=save.getmMainScene();
			mPlayer=save.getmPlayer();
			mapaName=save.getMapaName();
			mapas=save.getMapas();
			ois.close();
			changeMap(mapaName,mPlayer.getAnimatedSprite().getX(),mPlayer.getAnimatedSprite().getY());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ===========================================================
	// Fields
	// ===========================================================

	private HUD mHud;
	
	
	//Reproductor
    private MediaPlayer mp;
	// MENU BUTTON
	private BitmapTextureAtlas mInventoryMenuButtonTexture;
	private ITextureRegion mInventoryMenuButtonTextureRegion;
	private ButtonSprite mInventoryMenuButton;

	private BitmapTextureAtlas mSaveButtonTexture;
	private ITextureRegion mSaveButtonTextureRegion;
	private ButtonSprite mSaveButton;
	
	private BitmapTextureAtlas mOnScreenControlTexture;
	private ITextureRegion mOnScreenControlBaseTextureRegion;
	private ITextureRegion mOnScreenControlKnobTextureRegion;
	protected AnalogOnScreenControl mAnalogOnScreenControl;
	String message1="";
	int message2=0;
	private final Handler handler = new Handler() {
        @Override
		public void handleMessage(Message msg) {
              if(msg.arg1 == 1){
              Toast.makeText(getApplicationContext(),message1, Toast.LENGTH_LONG).show();
              }
              if(msg.arg1 == 2){
              Toast.makeText(getApplicationContext(),message2, Toast.LENGTH_LONG).show();
              }
        }
    };
	protected Inventory menu;
	protected boolean bMenu=false;
	// ===========================================================
	// Constructors
	// ===========================================================
	public ContactListener listener=new ContactListener(){

		@Override
		public void beginContact(Contact arg0) {
			mMainScene.setIgnoreUpdate(true);
//			Log.d("fixtureA", arg0.getFixtureA().getBody().getUserData().toString());
//			Log.d("fixtureB", arg0.getFixtureB().getBody().getUserData().toString());
			java.lang.Object fixtureA=arg0.getFixtureA().getBody().getUserData();
			java.lang.Object fixtureB=arg0.getFixtureB().getBody().getUserData(); 
			String A=fixtureA.toString();
			String B=fixtureB.toString();
			if(A.equals("player")){
				Player p=(Player) fixtureA;
				if(p.isAlive()){
					if(B.equals("object")){
						MapItem object=(MapItem) fixtureB;
						menu.addItem(object.getItem());
						object.detach(mMap.getmPhysicsWorld());
					}else if(B.equals("key")){
						Key key=(Key) fixtureB;
		  		    	  Door puerta=mapas.get(key.map).getDoors().get(key.door);
	         		    	if(key.abrir()&&!puerta.isAbierta()){
	         		    		
		           				puerta.abrir();
		           		    	key.detach(mMap.getmPhysicsWorld());
	         		    	}
	         		    	else if(key.cerrar()&&puerta.isAbierta()){
		           				puerta.cerrar();
		           		    	key.detach(mMap.getmPhysicsWorld());
	         		    	}
	         		   hablar(key.texto);
					}else if(B.equals("door")){
						Door door=(Door) fixtureB; 
	       				door.pasar(p);
					}else if(B.equals("attack")){
						Ataque ataque= (Ataque)fixtureB;
						if (!ataque.getCreator().toString().equals("player")){
							Character character=p;
							character.restVida(ataque.getDa�o()/character.cDefense);
							 mp = MediaPlayer.create(Game.this, R.raw.hurt);
						     mp.setLooping(false);
						     mp.start();
							if(ataque.getTypeAttack().equals("magicRanged")){
								ataque.detach(mMap.getmPhysicsWorld());
							}
						}
		       		}
				}
			}else if(A.equals("object")){
						if(B.equals("player")){
							Player p=(Player) fixtureB;
							if(p.isAlive()){
								MapItem object=(MapItem) fixtureA;
								menu.addItem(object.getItem());
								object.detach(mMap.getmPhysicsWorld());			
							}
						}
			}else if(A.equals("key")){
				if(B.equals("player")){
					Player p=(Player) fixtureB;
					if(p.isAlive()){
						Key key=(Key) fixtureA;
						
		  		    	  Door puerta=mapas.get(key.map).getDoors().get(key.door);
	         		    	if(key.abrir()&&!puerta.isAbierta()){
		           				puerta.abrir();
		           		    	key.detach(mMap.getmPhysicsWorld());
	         		    	}
	         		    	else if(key.cerrar()&&puerta.isAbierta()){
		           				puerta.cerrar();
		           		    	key.detach(mMap.getmPhysicsWorld());
	         		    	}
	         		   hablar(key.texto);
					}
				}
			}else if(A.equals("door")){
				if(B.equals("player")){

					Player p=(Player) fixtureB;
					if(p.isAlive()){
						Door door=(Door) fixtureA; 
	       				door.pasar(p);	
       				}
				}
			}else if(A.equals("attack")){
				if(B.equals("enemy")){
					Ataque ataque= (Ataque)fixtureA;
					if (!ataque.getCreator().toString().equals("enemy")){
						Character character=(Character)fixtureB;
						character.restVida(ataque.getDa�o()/character.cDefense);
						if(ataque.getTypeAttack().equals("magicRanged")){
							mp = MediaPlayer.create(Game.this, R.raw.volume);
						     mp.setLooping(false);
						     mp.start();
							ataque.detach(mMap.getmPhysicsWorld());
						}
					}
				}else if(B.equals("player")){
					Player p=(Player) fixtureB;
					if(p.isAlive()){
						Ataque ataque= (Ataque)fixtureA;
						if (!ataque.getCreator().toString().equals("player")){
							 mp = MediaPlayer.create(Game.this, R.raw.hurt);
						     mp.setLooping(false);
						     mp.start();
							Player character=p;
							character.restVida(ataque.getDa�o()/character.cDefense);
							if(ataque.getTypeAttack().equals("magicRanged")){
								ataque.detach(mMap.getmPhysicsWorld());
							}
						}
					}
				}else if(B.equals("wall")){
					Ataque ataque= (Ataque)fixtureA;
					ataque.detach(mMap.getmPhysicsWorld());
				}
			}else if(A.equals("enemy")){
				if(B.equals("attack")){
					Ataque ataque= (Ataque)fixtureB;
					if (!ataque.getCreator().toString().equals("enemy")){
						 mp = MediaPlayer.create(Game.this, R.raw.volume);
					     mp.setLooping(false);
					     mp.start();
						Character character=(Character)fixtureA;
						character.restVida(ataque.getDa�o()/character.cDefense);
						if(ataque.getTypeAttack().equals("magicRanged")){
							ataque.detach(mMap.getmPhysicsWorld());
						}
					}
				}
			}else if(A.equals("wall")){
				if(B.equals("attack")){
					Ataque ataque= (Ataque)fixtureB;
					if(ataque.getTypeAttack().equals("magicRanged")){
						ataque.detach(mMap.getmPhysicsWorld());
					}
				}
			}
			mMainScene.setIgnoreUpdate(false);
			

		}

		@Override
		public void endContact(Contact arg0) {
		
		}

		@Override
		public void postSolve(Contact arg0, ContactImpulse arg1) {

		}

		@Override
		public void preSolve(Contact arg0, Manifold arg1) {

			
		}
		
	};
	private Rectangle mVida;
	private float vRed=0;
	private float vGreen=1;
	private BitmapTextureAtlas mAtaqueMagicoButtonTexture;
	private TextureRegion mAtaqueMagicoButtonTextureRegion;
	private BitmapTextureAtlas mAtaqueMeleeButtonTexture;
	private TextureRegion mAtaqueMeleeButtonTextureRegion;
	private ButtonSprite mAtaqueMagicoButton;
	private ButtonSprite mAtaqueMeleeButton;
	private BitmapTextureAtlas mSeleccionAtaqueTexture;
	private TextureRegion mSeleccionAtaqueTextureRegion;
	private Sprite mSeleccionAtaque;

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {

		DisplayMetrics metrics= new DisplayMetrics();
		Display display= this.getWindowManager().getDefaultDisplay();
		display.getMetrics(metrics);
		CAMERA_WIDTH=(int) ((metrics.widthPixels)/metrics.xdpi*170);
		CAMERA_HEIGHT=(int) ((metrics.heightPixels)/metrics.ydpi*170);
		//Toast( CAMERA_WIDTH+"   "+CAMERA_HEIGHT);
		this.mBoundChaseCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		EngineOptions engineOptions= new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mBoundChaseCamera);

		engineOptions.getTouchOptions().setNeedsMultiTouch(true);

		mHud = new HUD();
		mBoundChaseCamera.setHUD(mHud);
		return engineOptions;
}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		menu= new Inventory(this);
		this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "knob.png", 128, 0);

		mInventoryMenuButtonTexture = new BitmapTextureAtlas(this.getTextureManager(), 55, 55, TextureOptions.NEAREST);
		mInventoryMenuButtonTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mInventoryMenuButtonTexture, this, "button-backpack-up.png", 0, 0);
		mInventoryMenuButtonTexture.load();
		mAtaqueMagicoButtonTexture = new BitmapTextureAtlas(this.getTextureManager(), 45, 45, TextureOptions.NEAREST);
		mAtaqueMagicoButtonTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mAtaqueMagicoButtonTexture, this, "ataqueMagico.png", 0, 0);
		mAtaqueMagicoButtonTexture.load();
		mAtaqueMeleeButtonTexture = new BitmapTextureAtlas(this.getTextureManager(), 45, 45, TextureOptions.NEAREST);
		mAtaqueMeleeButtonTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mAtaqueMeleeButtonTexture, this, "ataqueMelee.png", 0, 0);
		mAtaqueMeleeButtonTexture.load();

		mSeleccionAtaqueTexture = new BitmapTextureAtlas(this.getTextureManager(), 45, 45, TextureOptions.NEAREST);
		mSeleccionAtaqueTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mSeleccionAtaqueTexture, this, "seleccionAtaques.png", 0, 0);
		mSeleccionAtaqueTexture.load();
		
   		this.mOnScreenControlTexture.load();
		itemTest();
		
		
	}

	@Override
	public Scene onCreateScene() {

		this.mEngine.registerUpdateHandler(new FPSLogger());

		mMainScene = new Scene();
		BitmapTextureAtlas texture = new BitmapTextureAtlas(getTextureManager(), 1024, 768, TextureOptions.BILINEAR);
		ITextureRegion region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(texture, this, "cargando.png", 0, 0);
		texture.load();
		mMainScene.setBackground(new SpriteBackground(new Sprite(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, region,getVertexBufferObjectManager() )));
		return mMainScene;
	}
	@Override
	public boolean onKeyDown(final int pKeyCode, final KeyEvent pEvent) {
		if(pKeyCode == KeyEvent.KEYCODE_MENU && pEvent.getAction() == KeyEvent.ACTION_DOWN) {
			if(bMenu) {
				/* Remove the menu and reset it. */
				menu.getScene().back();
				bMenu=false;
				mMainScene.setChildScene(mAnalogOnScreenControl);
			} else {
				/* Attach the menu. */
				bMenu=true;
				
				this.mMainScene.setChildScene(this.menu.getScene(), false, true, true);
			}
			return true;
		} else {
			return super.onKeyDown(pKeyCode, pEvent);
		}
	}
	
	 @Override
	public void onBackPressed()
	    {
	        if(bMenu){
	            bMenu=false;
		    	mMainScene.setChildScene(mAnalogOnScreenControl);
	        }
	        else{
	        	new AlertDialog.Builder(this)
	        .setIcon(R.drawable.ic_launcher)
	        .setTitle(R.string.Exit)
	        .setMessage(R.string.ExitQuestion)
	        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
	    {
	        @Override
	        public void onClick(DialogInterface dialog, int which) {
	            finish();    
	        }

	    })
	    .setNegativeButton("No", null)
	    .show();
	        }
	    }

		public void Toast(String string) {
			if(!message1.equals(string)){
				this.message1=string;
				Message msg = handler.obtainMessage();
				msg.arg1 = 1;
				handler.sendMessage(msg);
			}
			
		}
		public void Toast(int message) {
			if(message2!=message){
				this.message2=message;
				Message msg = handler.obtainMessage();
				msg.arg1 = 2;
				handler.sendMessage(msg);
			}
			
		}
	
	public void itemTest(){
		menu.addItem("greataxe8hv.png", "axe2", 3, 2, "Weapon");
		menu.addItem("emeraldring4om.png", "ring2", 2, 2, "Accessory");
	}


	public int getCameraHeight() {
		return CAMERA_HEIGHT;
	}

	public int getCameraWidth() {
		return CAMERA_WIDTH;
	}
	@Override
	public Engine getEngine(){
		return mEngine;
	}

	public void changeMap(String name,final float x,final float y) {
		mapaName=name;

		mHud.setVisible(false);
		mAnalogOnScreenControl.setVisible(false);
		mPlayer.move(0, 0);
		mPlayer.direction=-1;
		
			mMainScene.registerUpdateHandler(new IUpdateHandler(){

				@Override
				public void onUpdate(float arg0) {
					mMainScene.detachChild(mMap.getMapScene());
					mMap.deletePlayer(mPlayer);
					mMap=mapas.get(mapaName);
					mMap.addPlayer(mPlayer,x, y);
					mMainScene.attachChild(mMap.getMapScene());
					mHud.setVisible(true);
					mAnalogOnScreenControl.setVisible(true);
					message1="";
					message2=0;
					mMainScene.unregisterUpdateHandler(this);
				}

				@Override
				public void reset() {
					// TODO Auto-generated method stub
					
				}
				
			});
		
	}
	
	@Override
	public void onGameCreated() {
		/* Calculate the coordinates for the face, so its centered on the camera. */
		final float centerX = (CAMERA_WIDTH) / 2;
		final float centerY = (CAMERA_HEIGHT) / 2;

		/* Create the sprite and add it to the scene. */
		mPlayer =new Player(centerX,centerY,"prota.png",this);
		this.mBoundChaseCamera.setChaseEntity(mPlayer.getAnimatedSprite());
		this.mapas.setGame(this);
		this.mMap=mapas.get(mapaName);
		mMap.addPlayer(mPlayer);
		this.mAnalogOnScreenControl = new AnalogOnScreenControl(20, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight()-20, 
				this.mBoundChaseCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, 
				this.getVertexBufferObjectManager(), mPlayer.getIOnScreenControlListener());
		this.mAnalogOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		this.mAnalogOnScreenControl.getControlBase().setAlpha(0.8f);
		this.mAnalogOnScreenControl.getControlBase().setScaleCenter(0, 128);
		this.mAnalogOnScreenControl.getControlBase().setScale(1.1f);
		this.mAnalogOnScreenControl.getControlKnob().setScale(1.1f);
		this.mAnalogOnScreenControl.refreshControlKnobPosition();
		mMainScene.attachChild(mMap.getMapScene());
		mMainScene.setChildScene(mAnalogOnScreenControl);
		menu.startMenu();
		Rectangle vida=new Rectangle(0, 0, 108, 16, mEngine.getVertexBufferObjectManager());
		vida.setVisible(true);
		vida.setColor(new Color(0, 0, 0));
		mHud.attachChild(vida);
		mVida=new Rectangle(4, 4, 100, 8, mEngine.getVertexBufferObjectManager());
		mVida.setVisible(true);
		mVida.setColor(new Color(vRed, vGreen, 0));
		mHud.attachChild(mVida);
		mAtaqueMagicoButton= new ButtonSprite(CAMERA_WIDTH -110, CAMERA_HEIGHT-60, mAtaqueMagicoButtonTextureRegion, mEngine.getVertexBufferObjectManager()) {
			@Override
			public boolean onAreaTouched(TouchEvent event, float x, float y)
			{

				mSeleccionAtaque.setPosition(CAMERA_WIDTH -110,CAMERA_HEIGHT-60);
				mPlayer.attack(0, -1);
				
				return super.onAreaTouched(event, x, y);
			}
		};
		mAtaqueMeleeButton= new ButtonSprite(CAMERA_WIDTH -50, CAMERA_HEIGHT-60, mAtaqueMeleeButtonTextureRegion, mEngine.getVertexBufferObjectManager()) {
			@Override
			public boolean onAreaTouched(TouchEvent event, float x, float y)
			{
				mSeleccionAtaque.setPosition(CAMERA_WIDTH -50,CAMERA_HEIGHT-60);
				mPlayer.attack(0, 1);
				return super.onAreaTouched(event, x, y);
			}
		};
		setmSeleccionAtaque(new Sprite(0, 0, mSeleccionAtaqueTextureRegion, mEngine.getVertexBufferObjectManager()));
				mInventoryMenuButton = new ButtonSprite(CAMERA_WIDTH -60,0, mInventoryMenuButtonTextureRegion, mEngine.getVertexBufferObjectManager()) {
			@Override
			public boolean onAreaTouched(TouchEvent event, float x, float y)
			{
				if ( event.isActionDown() )
				{
					if ( bMenu )
					{
						menu.getScene().back();
						bMenu=false;
						mMainScene.setChildScene(mAnalogOnScreenControl);
					}
					else
					{
						bMenu=true;
						
						Game.this.mMainScene.setChildScene(menu.getScene(), false, true, true);
					}
				}
				
				return super.onAreaTouched(event, x, y);
			}
		};
		mHud.registerTouchArea(mInventoryMenuButton);
		mHud.attachChild(mInventoryMenuButton);
		mHud.registerTouchArea(mAtaqueMagicoButton);
		mHud.attachChild(mAtaqueMagicoButton);
		mHud.registerTouchArea(mAtaqueMeleeButton);
		mHud.attachChild(mAtaqueMeleeButton);
		mHud.attachChild(getmSeleccionAtaque());
		getmSeleccionAtaque().setVisible(false);
		mMainScene.attachChild(mHud);
		mMainScene.setChildScene(mAnalogOnScreenControl);


	}

	public void gameOver() {		
		 finish();
		 Intent intentContinue = new Intent(Game.this , GameOver.class);
		 Game.this.startActivity(intentContinue);
	}

	public void setVida(int cVida) {
		vGreen=cVida/100f;
		vRed=1-vGreen;
		Log.d("vida",Integer.toString(cVida));
		mVida.setColor(new Color(vRed, vGreen, 0));
		mVida.setWidth(cVida);
	}
	
	public void hablar(String texto){
		//Implementar AlertDialog para las conversaciones (no funciona)
		
		/*if(texto.equals("anciano")){
			new AlertDialog.Builder(this)
	        .setIcon(R.drawable.caraanciano)
	        .setTitle("Anciano")
	        .setMessage(R.string.cantCross)
	        .setPositiveButton("Continuar", null)
	        .show();
		}*/

	}

	public Sprite getmSeleccionAtaque() {
		return mSeleccionAtaque;
	}
	@Override
	public void onPauseGame(){
		/* Attach the menu. */
		bMenu=true;
		
		this.mMainScene.setChildScene(this.menu.getScene(), false, true, true);
		
	}

	public void setmSeleccionAtaque(Sprite mSeleccionAtaque) {
		this.mSeleccionAtaque = mSeleccionAtaque;
	}

}

