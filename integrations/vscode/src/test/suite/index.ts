import * as path from 'path';
import * as glob from 'glob';
import * as Mocha from 'mocha';

export function run(): Promise<void> {
	// Create the mocha test
	const mocha = new Mocha({
		ui: 'tdd',
		color: true,
		reporter: 'mochawesome'
	});

	const testsRoot = path.resolve(__dirname, '..');
	
	const globPattern = process.env['TEST_GLOB_PATTER'] ? process.env['TEST_GLOB_PATTER'] : '**/*.test.js';
	console.log(globPattern);
	return new Promise((c, e) => {
		glob(globPattern, { cwd: testsRoot }, (err, files) => {
			if (err) {
				return e(err);
			}

			// Add files to the test suite
			files.forEach(f => mocha.addFile(path.resolve(testsRoot, f)));

			try {
				// Run the mocha test
				mocha.run(failures => {
					if (failures > 0) {
						e(new Error(`${failures} tests failed.`));
					} else {
						c();
					}
				});
			} catch (err) {
				console.error(err);
				e(err);
			}
		});
	});
}