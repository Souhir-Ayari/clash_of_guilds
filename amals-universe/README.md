# Amal's Little Universe

An interactive birthday card for Amal, made to be opened on a phone. Everything is in one file, `index.html` (HTML, CSS and JavaScript, no build step, no server, no audio files: the music is synthesized in the browser).

## The seven chapters

1. **The Invitation**: an envelope to open, her name, "Let's go".
2. **Her Soundtrack**: three moods (The Main Character, The Dreamer, The Artist). Music starts only when she presses a record; the button at the top right pauses it anywhere.
3. **Personality quiz**: six questions. The result is computed from her answers and names her two strongest traits (e.g. "The Colorful Human Connector").
4. **Amal, the Artist**: choose three colors, place stickers, draw with a finger. "Keep my creation" shows the image so she can save it (press and hold).
5. **Little Things About You**: a night sky with four glowing stars; each one reveals a message, and once all are lit they draw a glowing heart.
6. **A Letter For You**: a letter that writes itself line by line (with a "show the whole letter" button).
7. **The Finale**: blow out the candle, confetti, final message.

## Her song

The card plays "Falling For Your Eyes" (DA TI) softly in the background, starting on her first tap. It expects the file `song.mp3` next to `index.html`. The MP3 is not committed (copyrighted music in a public repo); add it yourself when publishing. Volume is `CONFIG.song.volume` (0.22).

## Personalize it

Open `index.html` and edit the `CONFIG` object at the top of the `<script>`:

- `from`: your first name, signed under the letter.
- `notes`: the four messages. Replace them with real memories, an inside joke, a precise moment. That is what makes the card hers.
- `letter`, `finale`, `moods`: the other texts.

## Publish it (GitHub Pages)

1. Put `index.html` in a public repository (this folder or a new repo).
2. GitHub → Settings → Pages → Source: "Deploy from a branch", choose the branch and folder.
3. The link looks like `https://<user>.github.io/<repo>/amals-universe/`. Send it to Amal.
