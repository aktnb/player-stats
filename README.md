# Player Stats

## 開発環境

JDK 25 を用意し、clone 後に以下を実行してコミット時の自動整形を有効にします。

```sh
git config core.hooksPath .githooks
```

`pre-commit` で Spotless / ktfmt を実行し、`src/**/*.kt` とルートの
`*.gradle.kts` を整形します。整形で変更が出た場合はコミットを停止します。
差分を確認して対象ファイルを `git add` し、もう一度コミットしてください。
未ステージの Kotlin の変更がある場合は、部分ステージを保護するため整形前に停止します。
先にステージするか、一時退避してください。

手動実行:

```sh
./gradlew spotlessApply # 整形
./gradlew spotlessCheck # 検証（build 時にも実行）
```
