package com.PrincipalPack;
import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;

public class TextoParpadeante {
	Context context;
	private TextView texto = null;
	private Animation fadeIn = null;
	private Animation fadeOut = null;
    // Listeners to detect the end of an animation
    private LocalFadeInAnimationListener myFadeInAnimationListener = new LocalFadeInAnimationListener();
    private LocalFadeOutAnimationListener myFadeOutAnimationListener = new LocalFadeOutAnimationListener();
    
    /**
     * Constructor
     * @param Context context
     * @param TextView text
     */
	public TextoParpadeante(Context context, TextView text){
		this.context = context;
		this.texto = text;
	    runAnimations();
	}

    /**
     * Performs the actual fade-out
     */
    private void launchOutAnimation() {
	    texto.startAnimation(fadeOut);
    }    
    
    /**
     * Performs the actual fade-in
     */
    private void launchInAnimation() {
	    texto.startAnimation(fadeIn);
    }    

    /**
     * Starts the animation
     */
    private void runAnimations() {  
    	//uso de las animaciones
	    fadeIn = AnimationUtils.loadAnimation(this.context, R.anim.fadein);
	    fadeIn.setAnimationListener( myFadeInAnimationListener );
	    fadeOut = AnimationUtils.loadAnimation(this.context, R.anim.fadeout);
	    fadeOut.setAnimationListener( myFadeOutAnimationListener ); 
	    // And start
    	launchInAnimation();
    }
    
    // Runnables to start the actual animation
    private Runnable mLaunchFadeOutAnimation = new Runnable() {
	    @Override
		public void run() {
	    	launchOutAnimation();
	    }
    };    
    
    private Runnable mLaunchFadeInAnimation = new Runnable() {
	    @Override
		public void run() {
	    	launchInAnimation();
	    }
    };    

    /**
     * Animation listener for fade-out
     * 
     * @author moi
     *
     */
    private class LocalFadeInAnimationListener implements AnimationListener {
	    @Override
		public void onAnimationEnd(Animation animation) {
		    texto.post(mLaunchFadeOutAnimation);
		}
	    @Override
		public void onAnimationRepeat(Animation animation){
	    }
	    @Override
		public void onAnimationStart(Animation animation) {
	    }
    };
    
    /**
     * Listener de animación para el Fadein
     */
    private class LocalFadeOutAnimationListener implements AnimationListener {
	    @Override
		public void onAnimationEnd(Animation animation) {
		    texto.post(mLaunchFadeInAnimation);
		}	
	    @Override
		public void onAnimationRepeat(Animation animation) {
	    }
	    @Override
		public void onAnimationStart(Animation animation) {
	    }
    };
}
