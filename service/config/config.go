package config

import (
	"log"
	"os"

	"github.com/jedib0t/go-pretty/v6/table"
	"github.com/spf13/viper"
)

type Configuration struct {
	MaxKeyCount uint64 `mapstructure:"max_key_cnt"`

	AijaAPIPort    int `mapstructure:"aija_port"`
	BrencisAPiPort int `mapstructure:"brencis_port"`
	AijaEnabled    bool
	BrencisEnabled bool

	Gatherer      string `mapstructure:"gatherer"`
	ClavisURL     string `mapstructure:"clavis_url"`
	FSGathererDir string `mapstructure:"fs_gatherer_dir"`

	LogInfo  bool `mapstructure:"log_info"`
	LogDebug bool `mapstructure:"log_debug"`

	DefaultServing bool `mapstructure:"default_serving"`
}

func LoadConfig() *Configuration {
	conf := getDefaultConfig()

	viper.SetConfigFile("config.toml")
	err := viper.ReadInConfig()
	if err != nil {
		log.Panic(err)
	}

	err = viper.Unmarshal(conf)
	if err != nil {
		log.Panic(err)
	}

	return conf
}

func getDefaultConfig() *Configuration {
	return &Configuration{
		MaxKeyCount:    1000000,
		AijaAPIPort:    8001,
		BrencisAPiPort: 8002,
		AijaEnabled:    true,
		BrencisEnabled: true,
		Gatherer:       "pseudorandom",
		LogInfo:        true,
		LogDebug:       true,
		DefaultServing: true,
	}
}

func (c *Configuration) Print() {
	t := table.NewWriter()
	t.SetOutputMirror(os.Stdout)
	t.AppendHeader(table.Row{"field name", "value"})
	t.AppendRows([]table.Row{
		{},
		{20, "Jon", "Snow", 2000, "You know nothing, Jon Snow!"},
	})
	t.Render()

}
