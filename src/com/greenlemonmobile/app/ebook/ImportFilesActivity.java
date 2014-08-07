
package com.greenlemonmobile.app.ebook;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.ebookdroid.CodecType;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenlemonmobile.app.ebook.entity.FileInfo;
import com.greenlemonmobile.app.ebook.entity.LocalBook;
import com.greenlemonmobile.app.utils.FileUtil;

public class ImportFilesActivity extends Activity implements OnClickListener, OnItemClickListener {

    private static final String LAST_SELECTED_FOLDER = "last_selected_folder";

    private TextView pathText;
    private ListView mList;
    private String folder;
    private ProgressDialog mImportProgressDialog;
    private FileSortHelper mFileSortHelper;
    private View mOperationPanel;

    private Button mImportBtn;

    public enum SortMethod {
        name, size, date, type
    }

    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();
    private FileListAdapter mAdapter;

    public class FileListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public FileListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mFileNameList.size();
        }

        @Override
        public Object getItem(int position) {
            return mFileNameList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        
        private int getFormatDrawable(String fileName) {
        	final File file = new File(fileName);
            final Uri data = Uri.fromFile(file);
            CodecType codecType = CodecType.getByUri(data.toString());
            
            switch (codecType) {
            case EPUB:
            	return R.drawable.list_file_epub;
            case TXT:
            	return R.drawable.list_file_txt;
            case PDF:
            	return R.drawable.list_file_pdf;
            case DJVU:
            	return R.drawable.list_file_djvu;
            case XPS:
            	return R.drawable.list_file_xps;
            case CBZ:
            	break;
            case CBR:
            	break;
            case FB2:
            	break;
            }

        	return R.drawable.list_file_default;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            ViewHolder holder = null;
            if (convertView != null) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                convertView = view = mInflater.inflate(R.layout.list_item_import_file, parent,
                        false);
                holder = new ViewHolder();
                holder.image = (ImageView) view.findViewById(R.id.image);
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.description = (TextView) view.findViewById(R.id.description);
                holder.checkbox = (CheckBox) view.findViewById(R.id.checkbox);
                view.setTag(holder);
            }
            convertView.setOnClickListener(null);
            convertView.setClickable(false);

            FileInfo file = (FileInfo) getItem(position);
            holder.name.setText(file.fileName);

            if (file.upback) {
                holder.checkbox.setVisibility(View.GONE);
                holder.description.setVisibility(View.GONE);
                holder.image.setImageResource(R.drawable.list_folder_parent);
            } else {
                holder.image.setImageResource(file.IsDir ? R.drawable.list_folder : getFormatDrawable(file.fileName));
                
                holder.checkbox.setVisibility(View.VISIBLE);
                holder.description.setVisibility(View.GONE);

                holder.checkbox.setTag(file);
                if (file.dbId == 0) {
                    holder.checkbox.setChecked(file.Selected);
                    holder.checkbox.setOnCheckedChangeListener(checkClick);
                } else {
                    holder.checkbox.setVisibility(View.GONE);
                    holder.description.setVisibility(View.VISIBLE);
                    holder.description.setText(getResources().getText(R.string.imported));
                }
            }
            return view;
        }

        private OnCheckedChangeListener checkClick = new OnCheckedChangeListener() {
        	
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
                FileInfo tag = (FileInfo) buttonView.getTag();
                tag.Selected = isChecked;
                updateSelection(tag);
			}
        };

        class ViewHolder {
            ImageView image;
            TextView name;
            TextView description;
            CheckBox checkbox;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_import_files);
        
        mOperationPanel = findViewById(R.id.operation_panel);
        mImportBtn = (Button) findViewById(R.id.library_synchronize_btn);
        mImportBtn.setOnClickListener(this);

        pathText = (TextView) findViewById(R.id.path_text);
        mList = (ListView) findViewById(R.id.filelist);

        mFileSortHelper = new FileSortHelper();
        mFileSortHelper.setSortMethod(SortMethod.name);

        mAdapter = new FileListAdapter(this);
        mList.setAdapter(mAdapter);
        mList.setClickable(true);
        mList.setOnItemClickListener(this);

        folder = PreferenceManager.getDefaultSharedPreferences(this).getString(
                LAST_SELECTED_FOLDER, Environment.getExternalStorageDirectory().getAbsolutePath());

        if (!folder.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            File file = new File(folder);
            if (!file.exists() || !file.isDirectory())
                folder = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        mList.setFastScrollEnabled(true);
        mList.setTextFilterEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFileNameList.clear();
        updateUI();
        refreshViewFileList();
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(LAST_SELECTED_FOLDER, folder).commit();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImportBtn.setOnClickListener(null);
        mList.setOnItemClickListener(null);
        mList = null;
    }

    private void showImportProgress(String msg) {
        mImportProgressDialog = new ProgressDialog(this);
        mImportProgressDialog.setMessage(msg);
        mImportProgressDialog.setIndeterminate(true);
        mImportProgressDialog.setCancelable(false);
        mImportProgressDialog.show();
    }

    private void closeImportProgress() {
        if (mImportProgressDialog != null) {
            mImportProgressDialog.dismiss();
            mImportProgressDialog = null;
        }
    }

    private void updateUI() {
        pathText.setText(folder);
    }

    private void refreshViewFileList() {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory())
            return;

        ArrayList<FileInfo> fileList = mFileNameList;
        fileList.clear();
        File[] listFiles = file.listFiles(documentFilter);
        if (listFiles == null)
            return;

        for (File child : listFiles) {

            String absolutePath = child.getAbsolutePath();
            if (FileUtil.isNormalFile(absolutePath) && FileUtil.shouldShowFile(absolutePath)) {
                FileInfo lFileInfo = FileUtil.GetFileInfo(child, documentFilter, false);
                if (lFileInfo != null) {
                     LocalBook book = LocalBook.getLocalBook(ImportFilesActivity.this, lFileInfo.filePath);
                     if (book != null && !lFileInfo.IsDir)
                    	 lFileInfo.dbId = 1;
                    fileList.add(lFileInfo);
                }
            }
        }
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                sortCurrentList(mFileSortHelper);
            }

        });
    }

    @SuppressWarnings("unchecked")
    public void sortCurrentList(FileSortHelper sort) {
        Collections.sort(mFileNameList, sort.getComparator());
        if (/* !folder.equals("/") */folder.lastIndexOf("/") != 0) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.fileName = "...";
            fileInfo.filePath = folder;
            fileInfo.upback = true;
            mFileNameList.add(0, fileInfo);
        }
        if (mFileNameList.size() > 0)
            mList.setSelection(0);

        mAdapter.notifyDataSetChanged();
    }

    private void updateSelection(FileInfo tag) {
//        if (tag.IsDir) {
//            for (FileInfo file : mFileNameList) {
//                String folder = new File(file.filePath).getParent();
//                if (tag.filePath.equals(folder) && file.dbId == 0 && !file.IsDir)
//                    file.Selected = tag.Selected;
//            }
//        } else {
//            boolean allSelected = true;
//            boolean hasSelected = false;
//            String folder = new File(tag.filePath).getParent();
//            for (FileInfo file : mFileNameList) {
//                String folder2 = new File(file.filePath).getParent();
//                if (!file.IsDir && folder2.equals(folder)) {
//                    if (file.Selected)
//                        hasSelected = true;
//                    else
//                        allSelected = false;
//                }
//            }
//
//            for (FileInfo file : mFileNameList) {
//                if (file.IsDir && file.filePath.equals(folder)) {
//                    if (!hasSelected && file.dbId == 0)
//                        file.Selected = false;
//                    else if (allSelected && file.dbId == 0)
//                        file.Selected = true;
//                    else
//                        file.Selected = allSelected;
//                    break;
//                }
//            }
//        }

        boolean hasSelectedItem = false;
//        int count = 0;
        for (FileInfo file : mFileNameList) {
            if (file.Selected/* && !file.IsDir*/) {
                hasSelectedItem = true;
//                ++count;
                break;
            }
        }
        mOperationPanel.setVisibility(hasSelectedItem ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.library_synchronize_btn:
                final ArrayList<FileInfo> fileList2BeImported = new ArrayList<FileInfo>();
                for (FileInfo file : mFileNameList) {
                    if (file.Selected/* && !file.IsDir*/) {
                        fileList2BeImported.add(file);
                    }
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        showImportProgress(getResources().getString(R.string.adding_book));
                    }

                });

                new Thread(new Runnable() {

                    @Override
                    public void run() {
                    	boolean hasFileImported = false;
                    	try {
	                        for (FileInfo fileInfo : fileList2BeImported) {
	                        	if (fileInfo.IsDir) {
	                        		File file = new File(fileInfo.filePath);
	                                File[] listFiles = file.listFiles(documentFilter);
	                                if (listFiles == null)
	                                    continue;

	                                for (File child : listFiles) {

	                                    String absolutePath = child.getAbsolutePath();
	                                    if (FileUtil.isNormalFile(absolutePath) && FileUtil.shouldShowFile(absolutePath)) {
	                                        FileInfo lFileInfo = FileUtil.GetFileInfo(child, documentFilter, false);
	                                        if (lFileInfo != null && !lFileInfo.IsDir) {
	                                        	LocalBook book = LocalBook.getLocalBook(ImportFilesActivity.this, lFileInfo.filePath);
	                                        	if (book == null) {
	                                        		LocalBook.importLocalBook(ImportFilesActivity.this, lFileInfo);
	                                        		hasFileImported = true;
	                                        	}
	                                        }
	                                    }
	                                }
	                        	}
	                        	else {
	                        		LocalBook.importLocalBook(ImportFilesActivity.this, fileInfo);
	                        		hasFileImported = true;
	                        	}
	                        }
                    	} catch (Exception e) {
                    		e.printStackTrace();
                    	}
                    	final boolean imprted = hasFileImported;
                        closeImportProgress();
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                            	if (!imprted)
                            		Toast.makeText(ImportFilesActivity.this, getResources().getText(R.string.import_popup_nothing_to_do), Toast.LENGTH_LONG).show();
                                setResult(Activity.RESULT_OK);
                                finish();
                            }

                        });
                    }

                }).start();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo file = mFileNameList.get(position);
        if (file == null)
            return;
        File f = new File(file.filePath);
        if (file.upback) {
            folder = f.getParent();
            updateUI();
            refreshViewFileList();
        } else {
            if (f.isDirectory()) {
                folder = file.filePath;
                updateUI();
                refreshViewFileList();
            } else {
            	file.Selected = !file.Selected;
            	if (file.Selected) {
            		updateSelection(file);
            	}
            	mAdapter.notifyDataSetChanged();
            }
        }
    }

    private FilenameExtFilter documentFilter = new FilenameExtFilter(DOCUMENT_EXTS);
    private static String[] DOCUMENT_EXTS = new String[] {
            "epub", "txt", "pdf", "djvu", "djv", "xps", "oxps", "cbz", "cbr", /*"fb2", "fb2.zip",*/ "chm" , "umd"
    };

    public class FilenameExtFilter implements FilenameFilter {

        private HashSet<String> mExts = new HashSet<String>();

        // using lower case
        public FilenameExtFilter(String[] exts) {
            if (exts != null) {
                mExts.addAll(Arrays.asList(exts));
            }
        }

        public boolean contains(String ext) {
            return mExts.contains(ext.toLowerCase());
        }

        @Override
        public boolean accept(File dir, String filename) {
            File file = new File(dir + File.separator + filename);

            if (!file.exists() || !file.canRead() || !file.canWrite())
                return false;

            if (file.isDirectory()) {
                return true;
            }

            int dotPosition = filename.lastIndexOf('.');
            if (dotPosition != -1) {
                String ext = (String) filename.subSequence(dotPosition + 1, filename.length());
                return contains(ext.toLowerCase());
            }

            return false;
        }
    }

    public class FileSortHelper {

        private SortMethod mSort;

        private boolean mFileFirst;

        private HashMap<SortMethod, Comparator> mComparatorList = new HashMap<SortMethod, Comparator>();

        public FileSortHelper() {
            mSort = SortMethod.name;
            mComparatorList.put(SortMethod.name, cmpName);
            mComparatorList.put(SortMethod.size, cmpSize);
            mComparatorList.put(SortMethod.date, cmpDate);
            mComparatorList.put(SortMethod.type, cmpType);
        }

        public void setSortMethod(SortMethod s) {
            mSort = s;
        }

        public SortMethod getSortMethod() {
            return mSort;
        }

        public void setFileFirst(boolean f) {
            mFileFirst = f;
        }

        public Comparator getComparator() {
            return mComparatorList.get(mSort);
        }

        private abstract class FileComparator implements Comparator<FileInfo> {

            @Override
            public int compare(FileInfo object1, FileInfo object2) {
                if (object1.IsDir == object2.IsDir) {
                    return doCompare(object1, object2);
                }

                if (mFileFirst) {
                    // the files are listed before the dirs
                    return (object1.IsDir ? 1 : -1);
                } else {
                    // the dir-s are listed before the files
                    return object1.IsDir ? -1 : 1;
                }
            }

            protected abstract int doCompare(FileInfo object1, FileInfo object2);
        }

        private Comparator cmpName = new FileComparator() {
            @Override
            public int doCompare(FileInfo object1, FileInfo object2) {
                return object1.fileName.compareToIgnoreCase(object2.fileName);
            }
        };

        private Comparator cmpSize = new FileComparator() {
            @Override
            public int doCompare(FileInfo object1, FileInfo object2) {
                return longToCompareInt(object1.fileSize - object2.fileSize);
            }
        };

        private Comparator cmpDate = new FileComparator() {
            @Override
            public int doCompare(FileInfo object1, FileInfo object2) {
                return longToCompareInt(object2.ModifiedDate - object1.ModifiedDate);
            }
        };

        private int longToCompareInt(long result) {
            return result > 0 ? 1 : (result < 0 ? -1 : 0);
        }

        private Comparator cmpType = new FileComparator() {
            @Override
            public int doCompare(FileInfo object1, FileInfo object2) {
                int result = FileUtil.getExtFromFilename(object1.fileName).compareToIgnoreCase(
                        FileUtil.getExtFromFilename(object2.fileName));
                if (result != 0)
                    return result;

                return FileUtil.getNameFromFilename(object1.fileName).compareToIgnoreCase(
                        FileUtil.getNameFromFilename(object2.fileName));
            }
        };
    }
}
