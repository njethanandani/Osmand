package net.osmand.plus.fragments;

import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.Algoritms;
import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.access.AccessibleToast;
import net.osmand.access.NavigationInfo;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.OpeningHoursParser;
import net.osmand.osm.OpeningHoursParser.OpeningHours;
import net.osmand.plus.AmenityTypeIcons;
import net.osmand.plus.NameFinderPoiFilter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PlaceType;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.R;
import net.osmand.plus.SearchByNameFilter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PlacePickerActivity;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchPOIActivity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Search poi activity
 */
public class SearchPOIFragment extends PlaceDetailsFragment /*implements SensorEventListener*/ {

	public static final String AMENITY_FILTER = "net.osmand.amenity_filter"; //$NON-NLS-1$
        public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT; //$NON-NLS-1$
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON; //$NON-NLS-1$
        private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	private static final int MIN_DISTANCE_TO_RESEARCH = 70;
	private static final int MIN_DISTANCE_TO_UPDATE = 6;

	private NavigationInfo navigationInfo;

	private Button searchPOILevel;
	//private ImageButton showOnMap;
	//private ImageButton showFilter;
	private PoiFilter filter;
	private AmenityAdapter amenityAdapter;
	private ListAdapter listAdapter;
	// private TextView searchArea;
	private EditText searchFilter;
	//private View searchFilterLayout;
	
	private boolean searchNearBy = false;
	private Location location = null; 
	private Float heading = null;
	
	private String currentLocationProvider = null;
	//private boolean sensorRegistered = false;
	//private Handler uiHandler;
	private OsmandSettings settings;
	private Path directionPath = new Path();
	private float width = 24;
	private float height = 24;
	
	// never null represents current running task or last finished
	private SearchAmenityTask currentSearchTask = new SearchAmenityTask(null); 
	
	private Application getApplication() {
	    return getActivity().getApplication();
	}
	
	private Context getContext() {
	    return SearchPOIFragment.this.getActivity();
	}
	
        @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
            navigationInfo = new NavigationInfo(getContext());
            
            View view;
            String filterId;
            if (getShownPlaceType() == PlaceTypesFragment.ADDRESS_SEARCH_KEY) {
                // Set up this fragment to act as 'search by name' POI filter 
                filterId = SearchByNameFilter.FILTER_ID;
                view = inflater.inflate(R.layout.search_by_name_fragment, null);
            } else {
                PlaceType placeType = PlaceTypesFragment.getPlaceTypesMap().get(getShownPlaceType());
                filterId = (placeType.getPoiFilterId() == null) ? placeType.getPoiFilterName() : placeType.getPoiFilterId();
                view = inflater.inflate(R.layout.search_poi_fragment, null);
            }
		
            //uiHandler = new Handler();
            searchPOILevel = (Button) view.findViewById(R.id.SearchPOILevelButton);
            // searchArea = (TextView) view.findViewById(R.id.SearchAreaText);
            searchFilter = (EditText) view.findViewById(R.id.SearchFilter);
            //searchFilterLayout = view.findViewById(R.id.SearchFilterLayout);
            //showOnMap = (ImageButton) view.findViewById(R.id.ShowOnMap);
            //showFilter = (ImageButton) view.findViewById(R.id.ShowFilter);
            directionPath = createDirectionPath();
		
            settings = ((OsmandApplication) getApplication()).getSettings();

            getActivity().getIntent().putExtra(SearchPOIActivity.AMENITY_FILTER, filterId);

            searchPOILevel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchAddress();
                }
            });
		
            // TODO(natashaj): Show filter not supported yet
            /*
            showFilter.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isSearchByNameFilter()){
                        Intent newIntent = new Intent(SearchPOIFragment.this.getActivity(), EditPOIFilterActivity.class);
                        newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, PoiFilter.CUSTOM_FILTER_ID);
                        if(location != null) {
                            newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, location.getLatitude());
                            newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, location.getLongitude());
                        }
                        startActivity(newIntent);
                    } else {
                    if (searchFilterLayout.getVisibility() == View.GONE) {
                        searchFilterLayout.setVisibility(View.VISIBLE);
                    } else {
                        searchFilter.setText(""); //$NON-NLS-1$
                        searchFilterLayout.setVisibility(View.GONE);
                    }
                    //}
                }
            });
            */
		
            searchFilter.addTextChangedListener(new TextWatcher(){
                @Override
                public void afterTextChanged(Editable s) {
                    if(!isNameFinderFilter() && !isSearchByNameFilter()){
                        amenityAdapter.getFilter().filter(s);
                    } else {
                        searchPOILevel.setEnabled(true);
                        searchPOILevel.setText(R.string.search_button);
                        // Cancel current search request here?
                    }
                }
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });
            
            searchFilter.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId,
                        KeyEvent event) {
                    if (actionId == EditorInfo.IME_NULL  
                            && event.getAction() == KeyEvent.ACTION_DOWN) { 
                            searchAddress(); // 'Enter' key results in same behavior as clicking 'Search' button
                    }
                    return true;
                }  
            });

            // TODO(natashaj): Show on map not supported yet
            /*
            showOnMap.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(searchFilter.getVisibility() == View.VISIBLE) {
                        filter.setNameFilter(searchFilter.getText().toString());
                    }
                    settings.setPoiFilterForMap(filter.getFilterId());
                    settings.SHOW_POI_OVER_MAP.set(true);
                    if(location != null){
                        settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(), 15);
                    }
                    MapActivity.launchMapActivityMoveToTop(SearchPOIActivity.this);
                }
            });
            */

            amenityAdapter = new AmenityAdapter(new ArrayList<Amenity>());
            setListAdapter(amenityAdapter);
		
            // ListActivity has a ListView, which you can get with:
            ListView lv = getListView(view);
            lv.setAdapter(getListAdapter());
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(position);
                    String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, getContext(), settings.usingEnglishNames());
                    String name = getString(R.string.poi)+" : " + poiSimpleFormat;
                    ((PlacePickerActivity)SearchPOIFragment.this.getActivity()).getPlacePickerListener().onPlacePicked(amenity.getLocation(), name);
                }
            });

            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                    final Amenity amenity = ((AmenityAdapter) SearchPOIFragment.this.getListAdapter()).getItem(pos);
                    onLongClick(amenity);
                    return true;
                }
            });
            
            return view;
        }
	
        private ListAdapter getListAdapter() {
            return this.listAdapter;
        }
        
        private void setListAdapter(ListAdapter listAdapter) {
            this.listAdapter = listAdapter;
        }
        
        private ListView getListView(View view) {
            return (ListView) view.findViewById(R.id.list);
        }
        
        private View findViewById(int id) {
            return getView().findViewById(id);
        }
        
        private void searchAddress() {
            String query = searchFilter.getText().toString();
            if (query.length() < 2 && (isNameFinderFilter() || isSearchByNameFilter())) {
                AccessibleToast.makeText(getContext(), R.string.poi_namefinder_query_empty, Toast.LENGTH_LONG).show();
                return;
            }
            
            InputMethodManager inputManager =(InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
            inputManager.hideSoftInputFromWindow(
                    this.getActivity().getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
            
            if(isNameFinderFilter() && 
                    !Algoritms.objectEquals(((NameFinderPoiFilter) filter).getQuery(), query)){
                filter.clearPreviousZoom();
                ((NameFinderPoiFilter) filter).setQuery(query);
                runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
            } else if(isSearchByNameFilter() && 
                    !Algoritms.objectEquals(((SearchByNameFilter) filter).getQuery(), query)){
                //showFilter.setVisibility(View.INVISIBLE);
                filter.clearPreviousZoom();
                showPoiCategoriesByNameFilter(query, location);
                ((SearchByNameFilter) filter).setQuery(query);
                runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.NEW_SEARCH_INIT));
            } else {
                runNewSearchQuery(SearchAmenityRequest.buildRequest(location, SearchAmenityRequest.SEARCH_FURTHER));
            }
        }
        
	private Path createDirectionPath() {
	    int h = 15;
	    int w = 4;
	    float sarrowL = 8; // side of arrow
	    float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
	    float hpartArrowL = (float) (harrowL - w) / 2;
	    Path path = new Path();
	    path.moveTo(width / 2, height - (height - h) / 3);
	    path.rMoveTo(w / 2, 0);
	    path.rLineTo(0, -h);
	    path.rLineTo(hpartArrowL, 0);
	    path.rLineTo(-harrowL / 2, -harrowL / 2); // center
	    path.rLineTo(-harrowL / 2, harrowL / 2);
	    path.rLineTo(hpartArrowL, 0);
	    path.rLineTo(0, h);
		
	    Matrix pathTransform = new Matrix();
	    WindowManager mgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
	    DisplayMetrics dm = new DisplayMetrics();
	    mgr.getDefaultDisplay().getMetrics(dm);
	    pathTransform.postScale(dm.density, dm.density);
	    path.transform(pathTransform);
	    width *= dm.density;
	    height *= dm.density;
	    return path;
	}

	@Override
        public void onResume() {
	    super.onResume();
	    Bundle bundle = getActivity().getIntent().getExtras();
	    if (bundle != null && bundle.containsKey(SEARCH_LAT) && bundle.containsKey(SEARCH_LON)){
	        location = new Location("internal"); //$NON-NLS-1$
	        location.setLatitude(bundle.getDouble(SEARCH_LAT));
	        location.setLongitude(bundle.getDouble(SEARCH_LON));
	        searchNearBy = false;
	    } else {
	        location = null;
	        searchNearBy = true;
	    }
		
	    String filterId = bundle.getString(AMENITY_FILTER);
	    PoiFilter filter = ((OsmandApplication) getApplication()).getPoiFilters().getFilterById(filterId);
	    if (filter != this.filter) {
	        this.filter = filter;
	        if (filter != null) {
	            filter.clearPreviousZoom();
	        } else {
	            amenityAdapter.setNewModel(Collections.<Amenity> emptyList(), "");
	            searchPOILevel.setText(R.string.search_POI_level_btn);
	            searchPOILevel.setEnabled(false);
	        }
	        // run query again
	        clearSearchQuery();
	    }
	    if(filter != null) {
	        filter.clearNameFilter();
	    }

	    /*
	    if(isNameFinderFilter()){
	        showFilter.setVisibility(View.GONE);
	        searchFilterLayout.setVisibility(View.VISIBLE);
	    } else if(isSearchByNameFilter() ){
	        showFilter.setVisibility(View.INVISIBLE);
	        searchFilterLayout.setVisibility(View.VISIBLE);
	    } else {
	        showFilter.setVisibility(View.VISIBLE);
	        showOnMap.setEnabled(filter != null);
	    }
	    showOnMap.setVisibility(View.VISIBLE);
	    */
	    
	    if (filter != null) {
	        // searchArea.setText(filter.getSearchArea());
	        updateSearchPoiTextButton();
	        
	        setLocation(location);
	        if (searchNearBy) {
	            LocationManager service = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
	            service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
	            currentLocationProvider = LocationManager.GPS_PROVIDER;
	            if (!isRunningOnEmulator()) {
	                // try to always ask for network provide it is faster way to find location
	                service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST,
	                        networkListener);
	                currentLocationProvider = LocationManager.NETWORK_PROVIDER;
	            }
	        }
	    }
	}
	
	private void showPoiCategoriesByNameFilter(String query, Location loc){
		OsmandApplication app = (OsmandApplication) getApplication();
		if(loc != null){
			Map<AmenityType, List<String>> map = app.getResourceManager().searchAmenityCategoriesByName(query, loc.getLatitude(), loc.getLongitude());
			if(!map.isEmpty()){
				PoiFilter filter = ((OsmandApplication)getApplication()).getPoiFilters().getFilterById(PoiFilter.CUSTOM_FILTER_ID);
				if(filter != null){
					//showFilter.setVisibility(View.VISIBLE);
					filter.setMapToAccept(map);
				}
				
				String s = typesToString(map);
				AccessibleToast.makeText(getContext(), getString(R.string.poi_query_by_name_matches_categories) + s, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private String typesToString(Map<AmenityType, List<String>> map) {
		StringBuilder b = new StringBuilder();
		int count = 0;
		Iterator<Entry<AmenityType, List<String>>> iterator = map.entrySet().iterator();
		while(iterator.hasNext() && count < 4){
			Entry<AmenityType, List<String>> e = iterator.next();
			b.append("\n").append(OsmAndFormatter.toPublicString(e.getKey(), getContext())).append(" - ");
			if(e.getValue() == null){
				b.append("...");
			} else {
				for(int j=0; j<e.getValue().size() && j < 3; j++){
					if(j > 0){
						b.append(", ");
					}
					b.append(e.getValue().get(j));
				}
			}
		}
		if(iterator.hasNext()){
			b.append("\n...");
		}
		return b.toString();
	}

	private void updateSearchPoiTextButton(){
		if(location == null){
			searchPOILevel.setText(R.string.search_poi_location);
			searchPOILevel.setEnabled(false);
		} else if (!isNameFinderFilter() && !isSearchByNameFilter()) {
			searchPOILevel.setText(R.string.search_POI_level_btn);
			searchPOILevel.setEnabled(currentSearchTask.getStatus() != Status.RUNNING &&
					filter.isSearchFurtherAvailable());
		} else {
			searchPOILevel.setText(R.string.search_button);
			searchPOILevel.setEnabled(currentSearchTask.getStatus() != Status.RUNNING && 
					filter.isSearchFurtherAvailable());
		}
	}
	
	public void setLocation(Location l){
		//registerUnregisterSensor(l);
		navigationInfo.setLocation(l);
		boolean handled = false;
		if (l != null && filter != null) {
			Location searchedLocation = getSearchedLocation();
			if (searchedLocation == null) {
  				searchedLocation = l;
				if (!isNameFinderFilter() && !isSearchByNameFilter()) {
					runNewSearchQuery(SearchAmenityRequest.buildRequest(l, SearchAmenityRequest.NEW_SEARCH_INIT));
				}
				handled = true;
			} else if (l.distanceTo(searchedLocation) > MIN_DISTANCE_TO_RESEARCH) {
				runNewSearchQuery(SearchAmenityRequest.buildRequest(l, SearchAmenityRequest.SEARCH_AGAIN));
				handled = true;
			} else if(location == null || location.distanceTo(l) > MIN_DISTANCE_TO_UPDATE){
				handled = true;
			}
		} else {
			if(location != null){
				handled = true;
			}
		}
		if(handled) {
			location = l;
			updateSearchPoiTextButton();
			amenityAdapter.notifyDataSetInvalidated();
		}
		
	}
	
	private Location getSearchedLocation(){
		return currentSearchTask.getSearchedLocation();
	}
	
	private synchronized void runNewSearchQuery(SearchAmenityRequest request){
		if(currentSearchTask.getStatus() == Status.FINISHED ||
				currentSearchTask.getSearchedLocation() == null){
			currentSearchTask = new SearchAmenityTask(request);
			currentSearchTask.execute();
		}
	}
	
	private synchronized void clearSearchQuery(){
		if(currentSearchTask.getStatus() == Status.FINISHED ||
				currentSearchTask.getSearchedLocation() == null){
			currentSearchTask = new SearchAmenityTask(null);
		}
	}
	
	
	private void onLongClick(final Amenity amenity) {
		String format = OsmAndFormatter.getPoiSimpleFormat(amenity, getContext(), settings.USE_ENGLISH_NAMES.get());
		if (amenity.getOpeningHours() != null) {
			AccessibleToast.makeText(getContext(), format + "  " + getString(R.string.opening_hours) + " : " + amenity.getOpeningHours(), Toast.LENGTH_LONG).show();
		}
		
		List<String> items = new ArrayList<String>();
		items.add(getString(R.string.show_poi_on_map));
		items.add(getString(R.string.navigate_to));
		if (((OsmandApplication)getApplication()).accessibilityEnabled())
			items.add(getString(R.string.show_details));

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(format);
		builder.setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 2){
					showPOIDetails(amenity, settings.usingEnglishNames());
					return;
				}
				if(which == 0){
					int z = settings.getLastKnownMapZoom();
					String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, getContext(), settings.usingEnglishNames());
					String name = getString(R.string.poi)+" : " + poiSimpleFormat;
					settings.setMapLocationToShow( 
							amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), 
							Math.max(16, z), name, name, amenity);
				} else if(which == 1){
					LatLon l = amenity.getLocation();
					String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, getContext(), settings.usingEnglishNames());
					settings.setPointToNavigate(l.getLatitude(), l.getLongitude(), getString(R.string.poi)+" : " + poiSimpleFormat);
				}
				MapActivity.launchMapActivityMoveToTop(getActivity());
				
			}
			
		});
		builder.show();
	}
	
	private boolean isRunningOnEmulator(){
		if (Build.DEVICE.equals("generic")) { //$NON-NLS-1$ 
			return true;
		}  
		return false;
	}
	
	public boolean isNameFinderFilter(){
		return filter instanceof NameFinderPoiFilter; 
	}
	
	public boolean isSearchByNameFilter(){
		return filter != null && PoiFilter.BY_NAME_FILTER_ID.equals(filter.getFilterId()); 
	}
	

	// TODO(natashaj): SensorEventListener not supported yet
	/*
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Attention : sensor produces a lot of events & can hang the system
		if(heading != null && Math.abs(heading - event.values[0]) < 4){
			// this is very small variation
			return;
		}
		heading = event.values[0];
		
		if(!uiHandler.hasMessages(5)){
			Message msg = Message.obtain(uiHandler, new Runnable(){
				@Override
				public void run() {
					amenityAdapter.notifyDataSetChanged();
				}
			});
			msg.what = 5;
			uiHandler.sendMessageDelayed(msg, 100);
		}
		
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	
	
	
	private void registerUnregisterSensor(Location location){
    	// show point view only if gps enabled
    	if(location == null){
    		if(sensorRegistered) {
    			Log.d(LogUtil.TAG, "Disable sensor"); //$NON-NLS-1$
    			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
    			sensorRegistered = false;
    			heading = null;
    		}
    	} else {
    		if(!sensorRegistered){
    			Log.d(LogUtil.TAG, "Enable sensor"); //$NON-NLS-1$
    			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
    			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    			if (s != null) {
    				sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    			}
    			sensorRegistered = true;
    		}
    	}
    }
    */
	
	@Override
    public void onPause() {
		super.onPause();
		if (searchNearBy) {
			LocationManager service = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
			service.removeUpdates(gpsListener);
			service.removeUpdates(networkListener);

			/*
			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
			sensorMgr.unregisterListener(this);
			sensorRegistered = false;
			currentLocationProvider = null;
			*/
		}
	}

	//@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		int z = settings.getLastKnownMapZoom();
		Amenity amenity = ((AmenityAdapter) getListAdapter()).getItem(position);
		String poiSimpleFormat = OsmAndFormatter.getPoiSimpleFormat(amenity, getContext(), settings.usingEnglishNames());
		String name = getString(R.string.poi)+" : " + poiSimpleFormat;
		settings.setMapLocationToShow( amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude(), 
				Math.max(16, z), name, name, amenity); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}
		
	static class SearchAmenityRequest {
		private static final int SEARCH_AGAIN = 1;
		private static final int NEW_SEARCH_INIT = 2;
		private static final int SEARCH_FURTHER = 3;
		private int type;
		private Location location;
		
		public static SearchAmenityRequest buildRequest(Location l, int type){
			SearchAmenityRequest req = new SearchAmenityRequest();
			req.type = type;
			req.location = l;
			return req;
			
		}
	}
	
	class SearchAmenityTask extends AsyncTask<Void, Amenity, List<Amenity>> implements ResultMatcher<Amenity> {
		
		private SearchAmenityRequest request;
		private TLongHashSet existingObjects = null;
		private final static int LIMIT_TO_LIVE_SEARCH = 150;
		
		public SearchAmenityTask(SearchAmenityRequest request){
			this.request = request;
			
		}
		
		Location getSearchedLocation(){
			return request != null ? request.location : null; 
		}

		@Override
		protected void onPreExecute() {
		        findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
			// findViewById(R.id.SearchAreaText).setVisibility(View.GONE);
			searchPOILevel.setEnabled(false);
			if(request.type == SearchAmenityRequest.NEW_SEARCH_INIT){
				amenityAdapter.clear();
			} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
				List<Amenity> list = amenityAdapter.getOriginalAmenityList();
				if (list.size() < LIMIT_TO_LIVE_SEARCH) {
					existingObjects = new TLongHashSet();
					for (Amenity a : list) {
						existingObjects.add(a.getId());
					}
				}
			}
		}
		
		@Override
		protected void onPostExecute(List<Amenity> result) {
		        findViewById(R.id.ProgressBar).setVisibility(View.GONE);
			// findViewById(R.id.SearchAreaText).setVisibility(View.VISIBLE);
			searchPOILevel.setEnabled(filter.isSearchFurtherAvailable());
			searchPOILevel.setText(R.string.search_POI_level_btn);
			if (isNameFinderFilter()) {
				if (!Algoritms.isEmpty(((NameFinderPoiFilter) filter).getLastError())) {
					AccessibleToast.makeText(getContext(), ((NameFinderPoiFilter) filter).getLastError(), Toast.LENGTH_LONG).show();
				}
				amenityAdapter.setNewModel(result, "");
				//showOnMap.setEnabled(amenityAdapter.getCount() > 0);
			} else if (isSearchByNameFilter()) {
				//showOnMap.setEnabled(amenityAdapter.getCount() > 0);
				amenityAdapter.setNewModel(result, "");
			} else {
				amenityAdapter.setNewModel(result, searchFilter.getText().toString());
			}
			// searchArea.setText(filter.getSearchArea());
		}
		
		@Override
		protected void onProgressUpdate(Amenity... values) {
			for(Amenity a : values){
				amenityAdapter.add(a);
			}
		}


		@Override
		protected List<Amenity> doInBackground(Void... params) {
			if (request.location != null) {
				if (request.type == SearchAmenityRequest.NEW_SEARCH_INIT) {
					return filter.initializeNewSearch(request.location.getLatitude(), request.location.getLongitude(), -1, this);
				} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
					return filter.searchFurther(request.location.getLatitude(), request.location.getLongitude(), this);
				} else if (request.type == SearchAmenityRequest.SEARCH_AGAIN) {
					return filter.searchAgain(request.location.getLatitude(), request.location.getLongitude());
				}
			}
			return Collections.emptyList();
		}

		@Override
		public boolean publish(Amenity object) {
			if(request.type == SearchAmenityRequest.NEW_SEARCH_INIT){
				publishProgress(object);
			} else if (request.type == SearchAmenityRequest.SEARCH_FURTHER) {
				if(existingObjects != null && !existingObjects.contains(object.getId())){
					publishProgress(object);
				}
			}
			
			return true;
		}
		
	}
	
	
	class AmenityAdapter extends ArrayAdapter<Amenity> {
		private AmenityFilter listFilter;
		private List<Amenity> originalAmenityList;
		AmenityAdapter(List<Amenity> list) {
			super(SearchPOIFragment.this.getActivity(), R.layout.searchpoi_list, list);
			originalAmenityList = new ArrayList<Amenity>(list);
			this.setNotifyOnChange(false);
		}
		
		public List<Amenity> getOriginalAmenityList() {
			return originalAmenityList;
		}

		public void setNewModel(List<Amenity> amenityList, String filter) {
			setNotifyOnChange(false);
			originalAmenityList = new ArrayList<Amenity>(amenityList);
			clear();
			for (Amenity obj : amenityList) {
				add(obj);
			}
			getFilter().filter(filter);
			setNotifyOnChange(true);
			this.notifyDataSetChanged();
			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoi_list, parent, false);
			}
			float[] mes = null;
			TextView label = (TextView) row.findViewById(R.id.poi_label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.poidistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.poi_icon);
			Amenity amenity = getItem(position);
			Location loc = location;
			if(loc != null){
				mes = new float[2];
				LatLon l = amenity.getLocation();
				Location.distanceBetween(l.getLatitude(), l.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
			}
                        int opened = -1;
                        if (amenity.getOpeningHours() != null) {
                                OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
                                if (rs != null) {
                                        Calendar inst = Calendar.getInstance();
                                        inst.setTimeInMillis(System.currentTimeMillis());
                                        boolean work = false;
                                        work = rs.isOpenedForTime(inst);
                                        if (work) {
                                                opened = 0;
                                        } else {
                                                opened = 1;
                                        }
                                }
                        }
                        
                        String str = OsmAndFormatter.getPoiNameOnly(amenity, settings.usingEnglishNames());
                        label.setText(str);
                        
			icon.setImageResource(AmenityTypeIcons.getIconResource(amenity));

			if(mes == null){
				distanceLabel.setText(""); //$NON-NLS-1$
			} else {
				distanceLabel.setText(" " + OsmAndFormatter.getFormattedDistance((int) mes[0], getContext())); //$NON-NLS-1$
			}
			return (row);
		}
		
		@Override
		public Filter getFilter() {
			if (listFilter == null) {
				listFilter = new AmenityFilter();
			}
			return listFilter;
		}
		
		private final class AmenityFilter extends Filter {
			
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				List<Amenity> listToFilter = originalAmenityList;
				if (constraint == null || constraint.length() == 0) {
					results.values = listToFilter;
					results.count = listToFilter.size();
				} else {
					String lowerCase = constraint.toString()
							.toLowerCase();
					List<Amenity> filter = new ArrayList<Amenity>();
					for (Amenity item : listToFilter) {
						String lower = OsmAndFormatter.getPoiStringWithoutType(item, settings.usingEnglishNames()).toLowerCase();
						if(lower.indexOf(lowerCase) != -1){
							filter.add(item);
						}
					}
					results.values = filter;
					results.count = filter.size();
				}
				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				clear();
				for (Amenity item : (Collection<Amenity>) results.values) {
					add(item);
				}
			}
		}
	}

	private void showPOIDetails(final Amenity amenity, boolean en) {
		AlertDialog.Builder b = new AlertDialog.Builder(getContext());
		b.setTitle(OsmAndFormatter.getPoiSimpleFormat(amenity, getContext(), en));
		b.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		List<String> attributes = new ArrayList<String>();
		String direction = navigationInfo.getDirectionString(amenity.getLocation(), heading);
		if (direction != null)
			attributes.add(direction);
		if (amenity.getPhone() != null) 
			attributes.add(getString(R.string.phone) + " " + amenity.getPhone());
		if (amenity.getOpeningHours() != null)
			attributes.add(getString(R.string.opening_hours) + " " + amenity.getOpeningHours());
		attributes.add(getString(R.string.navigate_point_latitude) + " " + Double.toString(amenity.getLocation().getLatitude()));
		attributes.add(getString(R.string.navigate_point_longitude) + " " + Double.toString(amenity.getLocation().getLongitude()));
		b.setItems(attributes.toArray(new String[attributes.size()]),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
		b.show();
	}

	// Working with location listeners
	private LocationListener networkListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location location) {
			setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			setLocation(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if(LocationProvider.OUT_OF_SERVICE == status){
				setLocation(null);
			}
		}
	};
	private LocationListener gpsListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location location) {
			setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			setLocation(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			LocationManager service = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
			// do not change provider for temporarily unavailable (possible bug for htc hero 2.1 ?)
			if (LocationProvider.OUT_OF_SERVICE == status /*|| LocationProvider.TEMPORARILY_UNAVAILABLE == status*/) {
				if(LocationProvider.OUT_OF_SERVICE == status){
					setLocation(null);
				}
				if (!isRunningOnEmulator() && service.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					if (!Algoritms.objectEquals(currentLocationProvider, LocationManager.NETWORK_PROVIDER)) {
						currentLocationProvider = LocationManager.NETWORK_PROVIDER;
						service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
					}
				}
			} else if (LocationProvider.AVAILABLE == status) {
				if (!Algoritms.objectEquals(currentLocationProvider, LocationManager.GPS_PROVIDER)) {
					currentLocationProvider = LocationManager.GPS_PROVIDER;
					service.removeUpdates(networkListener);
				}
			}
		}
	};
	
	class DirectionDrawable extends Drawable {
		Paint paintRouteDirection;
		
		private float angle;
		
		public DirectionDrawable(){
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(getResources().getColor(R.color.poi_direction));
			paintRouteDirection.setAntiAlias(true);
			
			
		}
		
		public void setOpenedColor(int opened){
			if(opened == 0){
				paintRouteDirection.setColor(getResources().getColor(R.color.poi_open));
			} else if(opened == -1){
				paintRouteDirection.setColor(getResources().getColor(R.color.poi_unknown_arrow));
			} else {
				paintRouteDirection.setColor(getResources().getColor(R.color.poi_closed));
			}
		}
		
		
		public void setAngle(float angle){
			this.angle = angle;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.rotate(angle, width/2, height/2);
			canvas.drawPath(directionPath, paintRouteDirection);
		}

		@Override
		public int getOpacity() {
			return 0;
		}

		@Override
		public void setAlpha(int alpha) {
			paintRouteDirection.setAlpha(alpha);
			
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paintRouteDirection.setColorFilter(cf);
		}
	}
}
