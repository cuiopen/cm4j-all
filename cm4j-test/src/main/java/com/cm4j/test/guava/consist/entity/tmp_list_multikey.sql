CREATE TABLE `tmp_list_multikey` (
  `n_player_id` int(11) NOT NULL,
  `n_type` int(11) NOT NULL,
  `n_value` int(11) NOT NULL,
  PRIMARY KEY (`n_player_id`,`n_type`)
)