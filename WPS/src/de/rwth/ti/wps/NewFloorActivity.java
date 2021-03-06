package de.rwth.ti.wps;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import de.rwth.ti.common.ChooseFileDialog;
import de.rwth.ti.common.Constants;
import de.rwth.ti.db.Building;
import de.rwth.ti.db.Floor;
import de.rwth.ti.layouthelper.BuildingSpinnerHelper;
import de.rwth.ti.layouthelper.OnBuildingChangedListener;

/**
 * 
 * This activity is used to create new buildings or floors
 * 
 */
public class NewFloorActivity extends SuperActivity implements
		OnBuildingChangedListener {

	private EditText createBuildingEdit;
	private Building selectedBuilding;
	private BuildingSpinnerHelper buildingHelper;
	private EditText floorLevelEdit;
	private EditText floorNameEdit;
	private EditText northEdit;
	private TextView floorFilenameView;
	private int floorLevel;
	private String floorName;
	private int north;
	private byte[] floorFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_floor);

		createBuildingEdit = (EditText) findViewById(R.id.dataBuildingRenameEdit);

		buildingHelper = BuildingSpinnerHelper.createInstance(this, this,
				getStorage(),
				(Spinner) findViewById(R.id.buildingSelectSpinner));

		floorLevelEdit = (EditText) findViewById(R.id.floorLevelEdit);
		floorNameEdit = (EditText) findViewById(R.id.floorNameEdit);
		northEdit = (EditText) findViewById(R.id.northEdit);
		// XXX remove default value and make visible
		northEdit.setText("0");
		northEdit.setVisibility(View.GONE);
		floorFilenameView = (TextView) findViewById(R.id.filePathEdit);

		TextWatcher textWatch = new TextWatcher() {
			public void afterTextChanged(Editable s) {
				onFloorLevelChanged();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			};

			public void onTextChanged(CharSequence s, int start, int count,
					int after) {
			};
		};
		floorLevelEdit.addTextChangedListener(textWatch);
		selectedBuilding = null;
		floorLevel = 0;
		floorName = null;
		north = -1;
		floorFile = null;
	}

	/** Called when the activity is first created or restarted */
	@Override
	public void onStart() {
		super.onStart();
		buildingHelper.refresh();
	}

	public void createBuilding(View view) {
		String buildingName = createBuildingEdit.getText().toString().trim();
		String message = null;
		Building newBuilding = null;
		boolean nameExists = false;
		// check name
		if (buildingName.length() != 0) {
			List<Building> buildingList = getStorage().getAllBuildings();
			for (Building tBuilding : buildingList) {
				if (buildingName.equals(tBuilding.getName())) {
					message = String.format(getString(R.string.name_existing),
							getString(R.string.building));
					nameExists = true;
					break;
				}
			}
			if (!nameExists) {
				newBuilding = getStorage().createBuilding(buildingName);

				// Gebäude konnte erfolgreich erstellt werden?
				if (newBuilding != null) {
					message = String.format(getString(R.string.success_create),
							getString(R.string.building));
					// Löscht den eingegeben Text
					createBuildingEdit.setText("");
					// Lädt die Liste der Gebäude neu
					buildingHelper.refresh();
					buildingHelper.setSelectedPosition(buildingName);
				} else {
					message = String.format(getString(R.string.error_create),
							getString(R.string.building));
				}
			}
		} else {
			message = getString(R.string.error_empty_input);
		}
		if (message != null) {
			// User message
			makeToast(message);
		}
	}

	@Override
	public void buildingChanged(BuildingSpinnerHelper helper) {
		selectedBuilding = helper.getSelectedBuilding();
	}

	private void onFloorLevelChanged() {
		String tFloorName = floorNameEdit.getText().toString().trim();
		int tFloorLevel;
		String tFloorLevelText = floorLevelEdit.getText().toString().trim();
		if (tFloorLevelText.equals("") || tFloorLevelText.equals("-")) {
			tFloorLevel = 0;
		} else {
			tFloorLevel = Integer.parseInt(tFloorLevelText);
		}
		if (tFloorName.equals("")
				|| tFloorName.equals(createFloorNameFromLevel(floorLevel))) {
			floorName = createFloorNameFromLevel(tFloorLevel);
			floorLevel = tFloorLevel;
			floorNameEdit.setText(floorName);
		}
	}

	public void createFloor(View view) {
		String message = null;
		floorName = floorNameEdit.getText().toString().trim();
		String tFloorLevelText = floorLevelEdit.getText().toString().trim();
		String tNorthText = northEdit.getText().toString().trim();
		Floor newFloor = null;
		boolean nameExists = false;

		// Kontrolliert, ob alle Inputs vernünftig gefüllt sind
		if (floorName.length() != 0 && tFloorLevelText.length() != 0
				&& !tFloorLevelText.equals("-") && tNorthText.length() != 0) {
			north = Integer.parseInt(tNorthText);

			// Überhaupt ein Gebäude vorhanden <=> Gebäude ausgewählt
			if (selectedBuilding != null) {
				// Kartendatei ausgewählt
				if (floorFile != null) {
					List<Floor> floorList = getStorage().getFloors(
							selectedBuilding);
					for (Floor tFloor : floorList) {
						if (floorName.equals(tFloor.getName())) {
							message = String.format(
									getString(R.string.name_existing),
									getString(R.string.floor));
							nameExists = true;
							break;
						}
					}
					if (!nameExists) {
						newFloor = getStorage().createFloor(selectedBuilding,
								floorName, floorFile, floorLevel, north);

						// Floor erfolgreich erstellt
						if (newFloor != null) {
							message = String.format(
									getString(R.string.success_create),
									getString(R.string.floor));
							// Eingaben löschen
							floorLevelEdit.setText("");
							floorLevelEdit.requestFocus();
							floorNameEdit.setText("");
							northEdit.setText("0");
							floorFilenameView.setText("");
							floorLevel = 0;
							floorName = null;
							north = -1;
							floorFile = null;
						} else {
							message = String.format(
									getString(R.string.error_create),
									getString(R.string.floor));
						}
					}
				} else {
					message = getString(R.string.error_no_floor_file);
				}
			} else {
				message = getString(R.string.error_no_building);
			}
		} else {
			message = getString(R.string.error_empty_input);
		}
		if (message != null) {
			// User message
			makeToast(message);
		}
	}

	public void chooseFile(View view) {
		// display choose file dialog
		ChooseFileDialog directoryChooserDialog = new ChooseFileDialog(
				NewFloorActivity.this,
				new ChooseFileDialog.ChosenFileListener() {
					@Override
					public void onChosenFile(String chosenFile) {
						File f = new File(chosenFile);
						if (f.exists() && f.isFile()) {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							FileInputStream fis = null;
							try {
								fis = new FileInputStream(f);
							} catch (FileNotFoundException e) {
								// XXX handle error
							}
							byte[] buffer = new byte[Constants.FILE_BUFFER_SIZE];
							try {
								for (int len = fis.read(buffer); len > 0; len = fis
										.read(buffer)) {
									baos.write(buffer);
								}
								fis.close();
							} catch (IOException ex) {
								// XXX handle error
							}
							floorFilenameView.setText(new File(chosenFile)
									.getName());
							floorFile = baos.toByteArray();
							try {
								baos.close();
							} catch (IOException ex) {
								// XXX handle error
							}
						}
					}
				}, Constants.MAP_SUFFIX);
		directoryChooserDialog.chooseDirectory(Constants.SD_APP_DIR);
	}
}
