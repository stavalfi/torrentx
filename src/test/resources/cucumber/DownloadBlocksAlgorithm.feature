#Feature: download blocks from fake-peers
#
#  Scenario Outline: (1) download blocks from a valid fake-peer
#    Given torrent: "<torrent>","<downloadLocation>"
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    Given link to "VALID" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
#      | 0  |
#      | -1 |
#    When application request the following blocks from all fake-peers - for torrent: "<torrent>":
#      | pieceIndex | from | length |
#      | 0          | 0    |        |
#      | -1         | 0    | 10     |
#      | 2          | 0    |        |
#    Then application receive the following blocks from all - for torrent: "<torrent>":
#      | pieceIndex | from | length |
#      | 0          | 0    |        |
#      | -1         | 0    | 10     |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
#
##  Scenario Outline: (2) download blocks from a multiple valid fake-peers
##    Given torrent: "<torrent>","<downloadLocation>"
##    When listen-status is trying to change to:
##      | START_LISTENING_IN_PROGRESS |
##    Given link to "VALID" - fake-peer on port "4041" with the following pieces - for torrent: "<torrent>"
##      | 0  |
##      | -1 |
##    Given link to "VALID" - fake-peer on port "4042" with the following pieces - for torrent: "<torrent>"
##      | 2  |
##      | -3 |
##    Given link to "VALID" - fake-peer on port "4043" with the following pieces - for torrent: "<torrent>"
##      | 4  |
##      | -5 |
##    When application request the following blocks from all fake-peers - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -1         | 0    | 10     |
##      | 2          | 0    |        |
##      | -3         | 0    | 10     |
##      | 4          | 0    |        |
##      | -5         | 0    |        |
##    Then application receive the following blocks from all - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -1         | 0    | 10     |
##      | 2          | 0    |        |
##      | -3         | 0    | 10     |
##      | 4          | 0    |        |
##      | -5         | 0    |        |
##
##    Examples:
##      | torrent                       | downloadLocation |
##      | torrent-file-example1.torrent | torrents-test    |
#
#
##  Scenario Outline: (3) download blocks from peer which closes the connection when he get the first request
##    Given torrent: "<torrent>","<downloadLocation>"
##    When listen-status is trying to change to:
##      | START_LISTENING_IN_PROGRESS |
##    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4044" with the following pieces - for torrent: "<torrent>"
##      | 0 |
##      | 1 |
##    When application request the following blocks from all fake-peers - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -2         | 0    |        |
##      | -1         | 0    | 10     |
##    Then application doesn't receive the following blocks from him - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -2         | 0    |        |
##      | -1         | 0    | 10     |
##    Then application receive the following blocks from all - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##
##    Examples:
##      | torrent                       | downloadLocation |
##      | torrent-file-example1.torrent | torrents-test    |
#
#  Scenario Outline: (4) download blocks from peer which closes the connection when he get the first request and valid peer
#    Given torrent: "<torrent>","<downloadLocation>"
#    When listen-status is trying to change to:
#      | START_LISTENING_IN_PROGRESS |
#    Given link to "CLOSE_IN_FIRST_REQUEST" - fake-peer on port "4045" with the following pieces - for torrent: "<torrent>"
#      | 0 |
#      | 1 |
#    Given link to "VALID" - fake-peer on port "4046" with the following pieces - for torrent: "<torrent>"
#      | 2 |
#      | 3 |
#    When application request the following blocks from all fake-peers - for torrent: "<torrent>":
#      | pieceIndex | from | length |
#      | 0          | 0    |        |
#      | 2          | 0    |        |
#      | 3          | 0    | 10     |
#    Then application doesn't receive the following blocks from him - for torrent: "<torrent>":
#      | pieceIndex | from | length |
#      | 0          | 0    |        |
#    Then application receive the following blocks from all - for torrent: "<torrent>":
#      | pieceIndex | from | length |
#      | 2          | 0    |        |
#      | 3          | 0    | 10     |
#
#    Examples:
#      | torrent                       | downloadLocation |
#      | torrent-file-example1.torrent | torrents-test    |
##
##
##  Scenario Outline: download the last blocks from a valid fake-peer which respond successfully with small delay
##    Given torrent: "<torrent>","<downloadLocation>"
##    Given link to "RESPOND_WITH_DELAY_100" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
##      | 0  |
##      | 1  |
##      # last block:
##      | -1 |
##      #second last block:
##      | -2 |
##    When torrent-status for torrent "<torrent>" is trying to change to:
##      | START_LISTENING_TO_INCOMING_PEERS |
##    When application request the following blocks from him - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -2         | 0    |        |
##      | -1         | 0    | 10     |
##    Then application receive the following blocks from him - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -2         | 0    |        |
##      | -1         | 0    | 10     |
##
##    Examples:
##      | torrent                       | downloadLocation |
##      | torrent-file-example1.torrent | torrents-test   |
##
##  Scenario Outline: download the last blocks from a valid fake-peer which response with too much delay
##    Given torrent: "<torrent>","<downloadLocation>"
##    Given link to "RESPOND_WITH_DELAY_3000" - fake-peer on port "4040" with the following pieces - for torrent: "<torrent>"
##      | 0  |
##      | 1  |
##      # last block:
##      | -1 |
##      #second last block:
##      | -2 |
##    When torrent-status for torrent "<torrent>" is trying to change to:
##      | START_LISTENING_TO_INCOMING_PEERS |
##    When application request the following blocks from him - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -2         | 0    |        |
##      | -1         | 0    | 10     |
##    Then application doesn't receive the following blocks from him - for torrent: "<torrent>":
##      | pieceIndex | from | length |
##      | 0          | 0    |        |
##      | -2         | 0    |        |
##      | -1         | 0    | 10     |
##
##    Examples:
##      | torrent                       | downloadLocation |
##      | torrent-file-example1.torrent | torrents-test   |
#
