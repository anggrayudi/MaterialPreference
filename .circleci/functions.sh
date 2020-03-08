function copyKeystore() {
  if [ "$CIRCLE_BRANCH" == "master" ]; then
    echo "$RELEASE_KEYSTORE_BASE_64" | base64 --decode > "$HOME/project/sample/keystore.jks"
  else
    echo "$DEBUG_KEYSTORE_BASE_64" | base64 --decode > "$HOME/.android/debug.keystore"
  fi
}