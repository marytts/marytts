package marytts.modules

import org.junit.*

import groovy.util.logging.Log4j

import marytts.LocalMaryInterface
import marytts.datatypes.MaryDataType

@Log4j
class PraatTextGridGeneratorIT {

	def mary

	@Before
	void setUp() {
		mary = new LocalMaryInterface()
		mary.outputType = MaryDataType.PRAAT_TEXTGRID.name
	}

	@Test
	void checkBoundaryTimes() {
		def textGrid = mary.generateText('Foo, bar.\n\nBaz!')
		def xmin = 0
		def xmax = 0
		def newInterval = false
		textGrid.eachLine { line->
			switch(line) {
				case ~/.*intervals \[\d\]:.*/:
					newInterval = true // begin interval block
					log.info "begin new interval block: $line"
					break
				case { line =~ /.*xmin =.*/ && newInterval }:
					xmin = Float.parseFloat(line.split().last())
					log.info "xmin = $xmin"
					assert xmin == xmax: "interval xmin must be equal to previous interval's xmax"
					break
				case { line =~ /.*xmax =.*/ && newInterval }:
					xmax = Float.parseFloat(line.split().last())
					log.info "xmax = $xmax"
					assert xmax > xmin: "interval xmax must be bigger than xmin"
					break
				case ~/.*text = .*/:
					newInterval = false // end interval block
					log.info "end new interval block: $line"
					break
				case ~/.*class = "IntervalTier".*/:
				// starting new tier; reset times
					xmin = 0
					xmax = 0
					break
				default:
				// ignore other lines
					break
			}
		}
	}
}
