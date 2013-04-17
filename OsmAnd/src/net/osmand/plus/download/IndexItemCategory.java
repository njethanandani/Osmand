package net.osmand.plus.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.IndexItem;

public class IndexItemCategory implements Comparable<IndexItemCategory> {
	public final String name;
	public final List<IndexItem> items = new ArrayList<IndexItem>();
	private final int order;

	public IndexItemCategory(String name, int order) {
		this.name = name;
		this.order = order;
	}

	@Override
	public int compareTo(IndexItemCategory another) {
		return order < another.order ? -1 : 1;
	}

	public static List<IndexItemCategory> categorizeIndexItems(Context ctx, Collection<IndexItem> indexItems) {
		final Map<String, IndexItemCategory> cats = new TreeMap<String, IndexItemCategory>();
		for (IndexItem i : indexItems) {
			int nameId = R.string.index_name_other;
			int order = 0;
			String lc = i.getFileName().toLowerCase();
			if (lc.endsWith(".voice.zip")) {
				nameId = R.string.index_name_voice;
				order = 1;
			} else if (lc.contains(".ttsvoice.zip")) {
				nameId = R.string.index_name_tts_voice;
				order = 2;
			} else if (lc.startsWith("us")) {
				nameId = R.string.index_name_us;
				order = 31;
			} else if (lc.contains("_northamerica_")) {
				nameId = R.string.index_name_north_america;
				order = 30;
			} else if (lc.contains("_centralamerica_") || lc.contains("central-america")) {
				nameId = R.string.index_name_central_america;
				order = 40;
			} else if (lc.contains("_southamerica_") || lc.contains("south-america")) {
				nameId = R.string.index_name_south_america;
				order = 45;
			} else if (lc.startsWith("france_")) {
				nameId = R.string.index_name_france;
				order = 17;
			} else if (lc.startsWith("germany_")) {
				nameId = R.string.index_name_germany;
				order = 16;
			} else if (lc.contains("_europe_")) {
				nameId = R.string.index_name_europe;
				order = 15;
			} else if (lc.startsWith("russia_")) {
				nameId = R.string.index_name_russia;
				order = 18;
			} else if (lc.contains("africa")) {
				nameId = R.string.index_name_africa;
				order = 80;
			} else if (lc.contains("_asia_")) {
				nameId = R.string.index_name_asia;
				order = 50;
			} else if (lc.contains("_oceania_") || lc.contains("australia")) {
				nameId = R.string.index_name_oceania;
				order = 70;
			} else if (lc.contains("_wiki_")) {
				nameId = R.string.index_name_wiki;
				order = 10;
			}

			String name = ctx.getString(nameId);
			if (!cats.containsKey(name)) {
				cats.put(name, new IndexItemCategory(name, order));
			}
			cats.get(name).items.add(i);
		}
		ArrayList<IndexItemCategory> r = new ArrayList<IndexItemCategory>(cats.values());
		Collections.sort(r);
		return r;
	}
}