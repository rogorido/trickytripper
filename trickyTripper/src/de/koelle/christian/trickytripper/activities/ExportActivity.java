package de.koelle.christian.trickytripper.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import de.koelle.christian.common.utils.UiUtils;
import de.koelle.christian.trickytripper.R;
import de.koelle.christian.trickytripper.TrickyTripperApp;
import de.koelle.christian.trickytripper.activitysupport.PopupFactory;
import de.koelle.christian.trickytripper.activitysupport.SpinnerViewSupport;
import de.koelle.christian.trickytripper.constants.Rc;
import de.koelle.christian.trickytripper.model.ExportSettings;
import de.koelle.christian.trickytripper.model.ExportSettings.ExportOutputChannel;
import de.koelle.christian.trickytripper.model.Participant;
import de.koelle.christian.trickytripper.ui.model.RowObject;

public class ExportActivity extends Activity {

    private List<Participant> participantsInSpinner;
    private Participant participantSelected;
    private ExportSettings exportSettings;
    private List<ExportOutputChannel> supportedOutputChannels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_view);
        initPanel();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.general_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.general_options_help:
            showDialog(Rc.DIALOG_SHOW_HELP);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        Dialog dialog;
        switch (id) {
        case Rc.DIALOG_SHOW_HELP:
            dialog = PopupFactory.createHelpDialog(this, getApp().getFktnController(), Rc.DIALOG_SHOW_HELP);
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    private TrickyTripperApp getApp() {
        return (TrickyTripperApp) getApplication();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
        case Rc.DIALOG_SHOW_HELP:
            // intentionally blank
            break;
        default:
            dialog = null;
        }
        super.onPrepareDialog(id, dialog, args);
    }

    private void initPanel() {
        final TrickyTripperApp app = getApp();
        exportSettings = app.getFktnController().getDefaultExportSettings();
        setTripName();
        initAndBindSpinner(app);
        supportedOutputChannels = app.getFktnController().getEnabledExportOutputChannel();
        initAndBindOutputChannelSpinner(exportSettings.getOutputChannel(), supportedOutputChannels);
        bindCheckBoxes();
        updateAllCheckboxStates();
        updateButtonState();
    }

    private void setTripName() {
        TextView textView = (TextView) findViewById(R.id.export_view_label_trip_name_output);
        textView.setText(getApp().getTripLoaded().getName());
    }

    private void initAndBindSpinner(final TrickyTripperApp app) {
        participantsInSpinner = new ArrayList<Participant>();
        participantsInSpinner.add(null);
        participantsInSpinner.addAll(app.getFktnController().getAllParticipants(false, true));

        Spinner spinner = SpinnerViewSupport.configureReportSelectionSpinner(
                this,
                this,
                R.id.reportViewBaseSpinner,
                participantsInSpinner);

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                participantSelected = participantsInSpinner.get(position);
                if (Log.isLoggable(Rc.LT_INPUT, Log.DEBUG)) {
                    Log.d(Rc.LT_INPUT, "selected=" + participantSelected.getName());
                }
                updateAllCheckboxStates();
            }

            public void onNothingSelected(AdapterView<?> parentView) {
                // intentionally blank
            }

        });
    }

    @SuppressWarnings("rawtypes")
    private void initAndBindOutputChannelSpinner(ExportOutputChannel selection,
            final List<ExportOutputChannel> enabledOnes) {
        final Spinner spinner = (Spinner) findViewById(R.id.exportViewSpinnerChannel);

        List<RowObject> spinnerObjects = SpinnerViewSupport.createSpinnerObjects(selection, false,
                null, getResources(), getApp().getFktnController()
                        .getDefaultStringCollator());
        ArrayAdapter<RowObject> adapter = new ArrayAdapter<RowObject>(this, android.R.layout.simple_spinner_item,
                spinnerObjects) {
            @Override
            public boolean isEnabled(int position) {
                return enabledOnes.contains(this.getItem(position).getRowObject());
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView result = (TextView) super.getView(position, convertView, parent);
                UiUtils.setActiveOrInactive(isEnabled(position), result, R.string.exportViewSpinnerNotAvailable,
                        getResources());
                return result;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView result = (TextView) super.getDropDownView(position, convertView, parent);
                UiUtils.setActiveOrInactive(isEnabled(position), result, R.string.exportViewSpinnerNotAvailable,
                        getResources());
                return result;
            }

        };
        adapter.setDropDownViewResource(R.layout.selection_list_medium);
        spinner.setPromptId(R.string.exportViewSpinnerPromptChannel);
        spinner.setAdapter(adapter);
        SpinnerViewSupport.setSelection(spinner, selection, adapter);

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @SuppressWarnings("unchecked")
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position >= 0) {
                    Object o = spinner.getSelectedItem();
                    ExportOutputChannel spinnerSelection = ((RowObject<ExportOutputChannel>) o).getRowObject();
                    ExportActivity.this.exportSettings.setOutputChannel(spinnerSelection);
                    updateButtonState();
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
                // intentionally blank
            }

        });
    }

    private void bindCheckBoxes() {
        CheckBox contentContentPayments = (CheckBox) findViewById(R.id.exportViewCheckboxContentPayments);
        CheckBox contentContentTransfers = (CheckBox) findViewById(R.id.exportViewCheckboxContentTransfers);
        CheckBox contentContentSpendingReport = (CheckBox) findViewById(R.id.exportViewCheckboxContentSpendingReport);
        CheckBox contentContentOwingDebts = (CheckBox) findViewById(R.id.exportViewCheckboxContentOwingDebts);
        CheckBox contentFormatCsv = (CheckBox) findViewById(R.id.exportViewCheckboxFormatCsv);
        CheckBox contentFormatHtml = (CheckBox) findViewById(R.id.exportViewCheckboxFormatHtml);
        CheckBox contentFormatTxt = (CheckBox) findViewById(R.id.exportViewCheckboxFormatTxt);
        CheckBox contentSeparateFilesForIndividuals = (CheckBox) findViewById(R.id.exportViewCheckboxSeparateFilesForIndividuals);
        CheckBox contentShowGlobalSumsOnIndividualSpendingReports = (CheckBox) findViewById(R.id.exportViewCheckboxShowTripSumOnIndividualSpendingReport);

        contentContentPayments.setChecked(exportSettings.isExportPayments());
        contentContentTransfers.setChecked(exportSettings.isExportTransfers());
        contentContentSpendingReport.setChecked(exportSettings.isExportSpendings());
        contentContentOwingDebts.setChecked(exportSettings.isExportDebts());
        contentFormatCsv.setChecked(exportSettings.isFormatCsv());
        contentFormatHtml.setChecked(exportSettings.isFormatHtml());
        contentFormatTxt.setChecked(exportSettings.isFormatTxt());
        contentSeparateFilesForIndividuals.setChecked(exportSettings.isSeparateFilesForIndividuals());
        contentShowGlobalSumsOnIndividualSpendingReports.setChecked(exportSettings
                .isShowGlobalSumsOnIndividualSpendingReport());

        contentContentPayments.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setExportPayments(isChecked);
                updateButtonState();
            }
        });
        contentContentTransfers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setExportTransfers(isChecked);
                updateButtonState();
            }
        });

        contentContentSpendingReport.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setExportSpendings(isChecked);
                updateButtonState();
                updateCheckboxStateShowGlobalSumsOnIndivudiualSpendings();
            }
        });

        contentContentOwingDebts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setExportDebts(isChecked);
                updateButtonState();
            }
        });

        contentFormatCsv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setFormatCsv(isChecked);
                updateButtonState();
            }
        });

        contentFormatHtml.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setFormatHtml(isChecked);
                updateButtonState();
            }
        });

        contentFormatTxt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setFormatTxt(isChecked);
                updateButtonState();
            }
        });
        contentSeparateFilesForIndividuals.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                exportSettings.setSeparateFilesForIndividuals(isChecked);
                updateCheckboxStateShowGlobalSumsOnIndivudiualSpendings();
            }
        });

        contentShowGlobalSumsOnIndividualSpendingReports
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        exportSettings.setShowGlobalSumsOnIndividualSpendingReport(isChecked);
                    }
                });

    }

    private void updateButtonState() {
        Button exportButton = (Button) findViewById(R.id.exportViewButtonDoExport);
        boolean buttonStateToBe = deriveEnableButtonStateFromSettings(exportSettings, supportedOutputChannels);
        exportButton.setEnabled(buttonStateToBe);

    }

    private void updateAllCheckboxStates() {
        updateCheckboxStateIndividualFiles();
        updateCheckboxStateShowGlobalSumsOnIndivudiualSpendings();
    }

    private void updateCheckboxStateIndividualFiles() {
        CheckBox checkbox = (CheckBox) findViewById(R.id.exportViewCheckboxSeparateFilesForIndividuals);
        boolean enabledToBe = deriveEnabledCheckboxStateFromSettings(participantSelected, exportSettings, true);
        checkbox.setEnabled(enabledToBe);
    }

    private void updateCheckboxStateShowGlobalSumsOnIndivudiualSpendings() {
        CheckBox checkbox = (CheckBox) findViewById(R.id.exportViewCheckboxShowTripSumOnIndividualSpendingReport);
        boolean enabledToBe = deriveEnabledCheckboxStateFromSettings(participantSelected, exportSettings, false);
        checkbox.setEnabled(enabledToBe);
    }

    private boolean deriveEnabledCheckboxStateFromSettings(Participant participantSelected2,
            ExportSettings exportSettings2,
            boolean individualFilesNotHideSums) {
        if (individualFilesNotHideSums) {
            return participantSelected2 == null;
        }
        else {
            return exportSettings2.isExportSpendings() &&
                    (participantSelected2 != null
                    || (participantSelected2 == null && exportSettings2.isSeparateFilesForIndividuals()));
        }
    }

    private boolean deriveEnableButtonStateFromSettings(ExportSettings exportSettings2,
            List<ExportOutputChannel> supportedOutputChannels2) {
        return (
                exportSettings2.isExportDebts()
                        || exportSettings2.isExportPayments()
                        || exportSettings2.isExportTransfers()
                        || exportSettings2.isExportSpendings()
                ) && (
                exportSettings2.isFormatTxt()
                        || exportSettings2.isFormatHtml()
                        || exportSettings2.isFormatCsv()
                ) && (
                supportedOutputChannels2.contains(exportSettings2.getOutputChannel())
                );
    }

    public void doExport(@SuppressWarnings("unused") View view) {
        /* participant selected is null, if nobody is selected. */
        /*
         * Files will be deleted on application's termination as usually files
         * have not be sent on resume here.
         */
        getApp().getFktnController().exportReport(exportSettings, participantSelected, this);
    }

}