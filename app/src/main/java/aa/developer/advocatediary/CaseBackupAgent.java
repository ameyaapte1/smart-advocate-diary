package aa.developer.advocatediary;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;

import java.io.File;

public class CaseBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate(){
        FileBackupHelper hosts = new FileBackupHelper(this,
                "../databases/" + CaseDatabaseHelper.DATABASE_NAME);
        addHelper(CaseDatabaseHelper.DATABASE_NAME, hosts);
    }

}