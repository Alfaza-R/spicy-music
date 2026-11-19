// Plugin sumber lagu: Apple iTunes Search API (preview 30 detik, tanpa API key)
spicy.registerSource({
  id: 'itunes',
  name: 'Preview iTunes',
  search: async function (q) {
    var r = await spicy.fetch('https://itunes.apple.com/search?media=music&entity=song&limit=40&term=' + encodeURIComponent(q));
    var j = await r.json();
    return (j.results || [])
      .filter(function (x) { return x.previewUrl; })
      .map(function (x) {
        return {
          id: x.trackId,
          title: x.trackName,
          artist: x.artistName,
          duration: 30,
          cover: (x.artworkUrl100 || '').replace('100x100', '300x300'),
          url: x.previewUrl
        };
      });
  }
});
