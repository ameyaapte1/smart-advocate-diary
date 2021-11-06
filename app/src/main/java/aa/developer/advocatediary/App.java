package aa.developer.advocatediary;

import android.app.Application;


/**
 * Created by ameyaapte1 on 12/2/17.
 */

public class App extends Application {

    private static App sInstance;


    public App() {
        sInstance = this;
    }

    public static App get() {
        return sInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //JobManager.create(this).addJobCreator(new CaseJobCreator());
    }
}
