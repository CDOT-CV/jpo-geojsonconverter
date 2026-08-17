package us.dot.its.jpo.geojsonconverter.converter.tim;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeSetXY;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Slf4j
public class TimConverter_NodeElevationAndWidthTest {



    @Test
    public void testElevationProfileMatchesNumberOfOfssetNodes_OneRegion() throws JsonProcessingException {
        testElevationAndWidthProfileNumberOfNodes(TIM_WITH_NODE_ELEVATION_AND_WIDTH);
    }

    @Test
    public void testElevationAndWidthProfileNumberOfNodes_TwoRegions() throws JsonProcessingException {
        testElevationAndWidthProfileNumberOfNodes(TIM_WITH_NODE_ELEVATION_AND_WIDTH_TWO_REGIONS);
    }

    private void testElevationAndWidthProfileNumberOfNodes(String travelerInformationJson) throws JsonProcessingException {
        var mapper = DateJsonMapper.getInstance();
        TravelerInformation tim = mapper.readValue(travelerInformationJson, TravelerInformation.class);
        OdeMessageFrameMetadata mockMetadata = new OdeMessageFrameMetadata();
        TimConverter timConverter = new TimConverter(new TimGeometryConverter());

        ProcessedTim processedTim = timConverter.createProcessedTim(tim, mockMetadata);
        String json = mapper.writeValueAsString(processedTim);
        log.info(json);
        int numberOfFrames = tim.getDataFrames().size();

        List<ProcessedTimFeature<?>> features = processedTim.getDataFrameFeatureCollection().getFeatures();
        assertThat("number of features should match number of data frames", features, hasSize(equalTo(numberOfFrames)));
        int numberOfFeatures = features.size();

        for (int frameNum = 0; frameNum < numberOfFrames; frameNum++) {
            TravelerDataFrame frame = tim.getDataFrames().get(frameNum);
            TravelerDataFrame.SequenceOfRegions paths = frame.getRegions();
            int numberOfPaths = paths.size();

            ProcessedTimFeature<?> feature = features.get(frameNum);

            List<ProcessedRegionInfoBase> regionInfoList = feature.getProperties().getRegionInfoList();
            assertThat("number of regions should match number of paths", regionInfoList, hasSize(equalTo(numberOfPaths)));

            for (int i = 0; i < numberOfPaths; i++) {

                ProcessedRegionInfoBase regionInfo = regionInfoList.get(i);
                assertThat(regionInfo, instanceOf(ProcessedPathRegionInfo.class));
                ProcessedPathRegionInfo pathRegionInfo = (ProcessedPathRegionInfo) regionInfo;

                GeographicalPath path = paths.get(i);
                NodeSetXY pathNodes = path.getDescription().getPath().getOffset().getXy().getNodes();
                int numPathNodes = pathNodes.size();

                // Check elevation profile
                ProcessedElevationProfile elevationProfile = pathRegionInfo.getElevationProfile();
                List<Double> elevationPoints = elevationProfile.getNodeElevationMeters();
                assertThat("number or elevation points should match number of nodes", elevationPoints, hasSize(equalTo(numPathNodes)));

                // Check width profile
                ProcessedLaneWidthProfile widthProfile = pathRegionInfo.getLaneWidthProfile();
                List<Double> widthPoints = widthProfile.getNodeLaneWidthMeters();
                assertThat("number or width points should match number of nodes", widthPoints, hasSize(equalTo(numPathNodes)));
            }
        }
    }

    // Traveler Information without message frame, with elevation and width offsets at two nodes
    public static final String TIM_WITH_NODE_ELEVATION_AND_WIDTH = """
            {
              "msgCnt": 0,
              "timeStamp": 331049,
              "packetID": "1A05652359A6EA9DCE",
              "dataFrames": [
                {
                  "doNotUse1": 0,
                  "frameType": "roadSignage",
                  "msgId": {
                    "furtherInfoID": "0000"
                  },
                  "startYear": 2025,
                  "startTime": 325144,
                  "durationTime": 32000,
                  "priority": 4,
                  "doNotUse2": 0,
                  "regions": [
                    {
                      "anchor": {
                        "lat": 337545852,
                        "long": -843986600,
                        "elevation": 1000
                      },
                      "laneWidth": 1500,
                      "directionality": "forward",
                      "closedPath": false,
                      "description": {
                        "path": {
                          "offset": {
                            "xy": {
                              "nodes": [
                                {
                                  "delta": {
                                    "node-XY2": {
                                      "x": 926,
                                      "y": 0
                                    }
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY5": {
                                      "x": 2895,
                                      "y": 6794
                                    }
                                  },
                                  "attributes": {
                                    "dWidth": 50,
                                    "dElevation": 200
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY5": {
                                      "x": 3248,
                                      "y": 4543
                                    }
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY4": {
                                      "x": 4095,
                                      "y": 3439
                                    }
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY5": {
                                      "x": 5084,
                                      "y": 2462
                                    }
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY6": {
                                      "x": 11439,
                                      "y": 4501
                                    }
                                  },
                                  "attributes": {
                                    "dWidth": 50,
                                    "dElevation": -300
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY5": {
                                      "x": 6355,
                                      "y": 3567
                                    }
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY5": {
                                      "x": 5154,
                                      "y": 4161
                                    }
                                  }
                                },
                                {
                                  "delta": {
                                    "node-XY5": {
                                      "x": 7908,
                                      "y": 6964
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      }
                    }
                  ],
                  "doNotUse3": 0,
                  "doNotUse4": 0,
                  "content": {
                    "advisory": [
                      {
                        "item": {
                          "itis": 769
                        }
                      },
                      {
                        "item": {
                          "itis": 9478
                        }
                      },
                      {
                        "item": {
                          "itis": 7747
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """;

    public static final String TIM_WITH_NODE_ELEVATION_AND_WIDTH_TWO_REGIONS = """
            {
                "msgCnt": 0,
                "timeStamp": 331049,
                "packetID": "1A05652359A6EA9DCE",
                "dataFrames": [
                    {
                        "doNotUse1": 0,
                        "frameType": "roadSignage",
                        "msgId": {
                            "furtherInfoID": "0000"
                        },
                        "startYear": 2025,
                        "startTime": 325144,
                        "durationTime": 32000,
                        "priority": 4,
                        "doNotUse2": 0,
                        "regions": [
                            {
                                "anchor": {
                                    "lat": 337545852,
                                    "long": -843986600,
                                    "elevation": 1000
                                },
                                "laneWidth": 1500,
                                "directionality": "forward",
                                "closedPath": false,
                                "description": {
                                    "path": {
                                        "offset": {
                                            "xy": {
                                                "nodes": [
                                                    {
                                                        "delta": {
                                                            "node-XY2": {
                                                                "x": 926,
                                                                "y": 0
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 2895,
                                                                "y": 6794
                                                            }
                                                        },
                                                        "attributes": {
                                                            "dWidth": 50,
                                                            "dElevation": 200
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 3248,
                                                                "y": 4543
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY4": {
                                                                "x": 4095,
                                                                "y": 3439
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 5084,
                                                                "y": 2462
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY6": {
                                                                "x": 11439,
                                                                "y": 4501
                                                            }
                                                        },
                                                        "attributes": {
                                                            "dWidth": 50,
                                                            "dElevation": -300
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 6355,
                                                                "y": 3567
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 5154,
                                                                "y": 4161
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 7908,
                                                                "y": 6964
                                                            }
                                                        }
                                                    }
                                                ]
                                            }
                                        }
                                    }
                                }
                            },
                            {
                                "anchor": {
                                    "lat": 337583694,
                                    "long": -843928924,
                                    "elevation": -4096
                                },
                                "laneWidth": 1500,
                                "directionality": "forward",
                                "closedPath": false,
                                "description": {
                                    "path": {
                                        "offset": {
                                            "xy": {
                                                "nodes": [
                                                    {
                                                        "delta": {
                                                            "node-XY2": {
                                                                "x": -857,
                                                                "y": -517
                                                            }
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": -5405,
                                                                "y": -3262
                                                            }
                                                        },
                                                        "attributes": {
                                                            "dWidth": 100,
                                                            "dElevation": -200
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY4": {
                                                                "x": -3091,
                                                                "y": 2816
                                                            }
                                                        },
                                                        "attributes": {
                                                            "dWidth": 100,
                                                            "dElevation": -200
                                                        }
                                                    },
                                                    {
                                                        "delta": {
                                                            "node-XY5": {
                                                                "x": 6206,
                                                                "y": 2139
                                                            }
                                                        },
                                                        "attributes": {
                                                            "dWidth": -100,
                                                            "dElevation": 300
                                                        }
                                                    }
                                                ]
                                            }
                                        }
                                    }
                                }
                            }
                        ],
                        "doNotUse3": 0,
                        "doNotUse4": 0,
                        "content": {
                            "advisory": [
                                {
                                    "item": {
                                        "itis": 769
                                    }
                                },
                                {
                                    "item": {
                                        "itis": 9478
                                    }
                                },
                                {
                                    "item": {
                                        "itis": 7747
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
            """;
}
